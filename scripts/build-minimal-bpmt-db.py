#!/usr/bin/env python3
import argparse
import re
from pathlib import Path
from xml.etree import ElementTree as ET
from zipfile import ZipFile


SPREADSHEET_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
NS = {"a": SPREADSHEET_NS, "r": REL_NS}
CELL_REF_RE = re.compile(r"([A-Z]+)([0-9]+)")
HEADER_RE = re.compile(r"^\[([^\]]+)\]")


def column_index(cell_ref):
    match = CELL_REF_RE.match(cell_ref)
    if not match:
        return 0
    index = 0
    for char in match.group(1):
        index = index * 26 + ord(char) - ord("A") + 1
    return index


def read_shared_strings(zip_file):
    shared_strings = []
    root = ET.fromstring(zip_file.read("xl/sharedStrings.xml"))
    for item in root.findall("a:si", NS):
        parts = []
        for text in item.iter("{%s}t" % SPREADSHEET_NS):
            parts.append(text.text or "")
        shared_strings.append("".join(parts))
    return shared_strings


def workbook_sheets(zip_file):
    workbook = ET.fromstring(zip_file.read("xl/workbook.xml"))
    rels = ET.fromstring(zip_file.read("xl/_rels/workbook.xml.rels"))
    rel_targets = {rel.attrib["Id"]: rel.attrib["Target"] for rel in rels}

    for sheet in workbook.find("a:sheets", NS):
        name = sheet.attrib["name"]
        rel_id = sheet.attrib["{%s}id" % REL_NS]
        yield name, "xl/" + rel_targets[rel_id]


def cell_value(cell, shared_strings):
    value = cell.find("a:v", NS)
    if value is None or value.text is None:
        return ""
    raw = value.text
    if cell.attrib.get("t") == "s":
        return shared_strings[int(raw)]
    return raw


def read_sheet_rows(zip_file, sheet_path, shared_strings):
    root = ET.fromstring(zip_file.read(sheet_path))
    rows = []
    for row in root.findall(".//a:sheetData/a:row", NS):
        values = {}
        for cell in row.findall("a:c", NS):
            index = column_index(cell.attrib.get("r", "A1"))
            values[index] = cell_value(cell, shared_strings)
        if values:
            rows.append(values)
    return rows


def parse_workbook(path):
    tables = []
    with ZipFile(path) as zip_file:
        shared_strings = read_shared_strings(zip_file)
        for table_name, sheet_path in workbook_sheets(zip_file):
            rows = read_sheet_rows(zip_file, sheet_path, shared_strings)
            if not rows:
                continue

            header_row = rows[0]
            max_column = max(header_row)
            columns = []
            for index in range(1, max_column + 1):
                header = header_row.get(index, "")
                match = HEADER_RE.match(header)
                if match:
                    columns.append((index, match.group(1)))

            data_rows = []
            for row in rows[1:]:
                values = [row.get(index, "") for index, _ in columns]
                if any(value != "" for value in values):
                    data_rows.append(values)

            tables.append((table_name, [name for _, name in columns], data_rows))
    return tables


def sql_literal(value):
    if value == "":
        return "NULL"
    return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'"


def insert_sql(table_name, columns, rows):
    if not rows:
        return []

    quoted_columns = ", ".join("`%s`" % column for column in columns)
    statements = []
    for values in rows:
        quoted_values = ", ".join(sql_literal(value) for value in values)
        statements.append("INSERT INTO `%s` (%s) VALUES (%s);" % (table_name, quoted_columns, quoted_values))
    return statements


def build_sql(ddl_path, workbook_path, output_path):
    ddl = ddl_path.read_text(encoding="utf-8")
    tables = parse_workbook(workbook_path)

    output = []
    output.append("-- bpmt-lite minimal database")
    output.append("-- schema source: %s" % ddl_path)
    output.append("-- data source: %s" % workbook_path)
    output.append("SET NAMES utf8;")
    output.append("SET FOREIGN_KEY_CHECKS=0;")
    output.append("")
    output.append("CREATE DATABASE IF NOT EXISTS `kyq` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;")
    output.append("USE `kyq`;")
    output.append("")
    output.append(ddl.rstrip())
    output.append("")
    output.append("-- minimal init data")
    for table_name, columns, rows in tables:
        output.append("")
        output.append("-- %s: %d rows" % (table_name, len(rows)))
        output.extend(insert_sql(table_name, columns, rows))
    output.append("")
    output.append("SET FOREIGN_KEY_CHECKS=1;")
    output.append("")

    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text("\n".join(output), encoding="utf-8")


def main():
    parser = argparse.ArgumentParser(description="Build minimal bpmt database SQL from hbm2ddl MySQL DDL and init-data workbook.")
    parser.add_argument("--ddl", default="/Users/wenzhewang/workspace/bpmt_project/riversoft/trunk/support/hbm2ddl/target/sql-bpmt-lite/mysql/create_model.sql")
    parser.add_argument("--xlsx", default="/Users/wenzhewang/workspace/bpmt_project/riversoft/package/database/bpmt_init_data.xlsx")
    parser.add_argument("--output", default="database/bpmt-db.sql")
    args = parser.parse_args()

    build_sql(Path(args.ddl), Path(args.xlsx), Path(args.output))


if __name__ == "__main__":
    main()
