package me.asu.blog;

import junit.framework.TestCase;

public class ConfigTest extends TestCase
{

	public void testLoad() {
		Config c= new Config();
		c.list(System.out);
	}
}