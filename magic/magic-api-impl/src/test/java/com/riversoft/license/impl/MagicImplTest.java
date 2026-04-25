/*
 * $HeadURL: $
 * $Id: $
 * Copyright (c) 2013 by Riversoft System, all rights reserved.
 */
package com.riversoft.license.impl;

import com.riversoft.util.jackson.JsonMapper;
import org.junit.Assert;
import org.junit.Test;

import com.riversoft.license.api.Identifier;
import com.riversoft.license.api.Magic;

/**
 * @author Borball
 * 
 */
public class MagicImplTest {

    @Test
    public void testRead() {
        Magic magic = new MagicImpl();

        Identifier identifier = magic.currentIdentifier();
        Assert.assertEquals("test-user", identifier.getName());
        Assert.assertEquals("test-password", identifier.getPassword());
        Assert.assertEquals("test-platform", identifier.getPlatform());
        Assert.assertEquals(0, identifier.getMaxSessions());
        Assert.assertEquals(1, identifier.getLevel());
        Assert.assertEquals("test-identifier", identifier.getIdentifier());
        Assert.assertEquals("test-skey", identifier.getSkey());
        Assert.assertFalse(identifier.isCommercial());
        Assert.assertFalse(identifier.isRegister());
    }
    
    @Test
    public void testWrite() {
        Identifier identifier = new Identifier();
        identifier.setName("test-user");
        identifier.setPlatform("test-platform");
        identifier.setLevel(1);
        identifier.setSkey("test-skey");
        identifier.setPassword("test-password");
        identifier.setCommercial(false);
        identifier.setRegister(false);

        System.out.println(JsonMapper.defaultMapper().toJson(identifier));
    }

}
