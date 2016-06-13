package com.bol.logback;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class RedisAppenderTest {

	private String key = "logstash";
	private Jedis redis;

	@Test
	public void logTest() throws Exception {
		// refer to logback.xml in test folder
		configLogger("/logback.xml");
		Logger logger = LoggerFactory.getLogger(RedisAppenderTest.class);
		logger.debug("Test Log #1");
		logger.debug("Test Log #2");
		logger.debug("Test Log #3");
		logger.debug("Test Log #4");
		logger.debug("Test Log #5");

		// list length check
		long len = redis.llen(key);
		assertEquals(5L, len);

		// Use Jackson to check JSON content
		String content = redis.lpop(key);
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(content);

		assertEquals("test-application", node.get("source").asText());
		assertEquals("Test Log #1", node.get("@message").asText());
	}

	@Test
	public void logTestMDC() throws Exception {
		// refer to logback-mdc.xml in test folder
		configLogger("/logback-mdc.xml");
		Logger logger = LoggerFactory.getLogger(RedisAppenderTest.class);
		MDC.put("mdcvar1", "test1");
		MDC.put("mdcvar2", "test2");
		logger.debug("Test MDC Log");

		String content = redis.lpop(key);
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(content);
		ArrayNode tags = (ArrayNode) node.get("tags");
		assertEquals("test1", tags.get(0).asText());
		assertEquals("test2", tags.get(1).asText());
		assertEquals("test1 test2 mdcvar3_NOT_FOUND", tags.get(2).asText());
	}

	@Before
	public void setUp() {
		System.out.println("Before Test, clearing Redis");
		JedisPool pool = new JedisPool("localhost");
		redis = pool.getResource();
		// clear the redis list first
		redis.ltrim(key, 1, 0);
	}

	private void configLogger(String loggerxml) {
		LoggerContext context = (LoggerContext) LoggerFactory
				.getILoggerFactory();

		try {
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(context);
			context.reset();
			configurator.doConfigure(this.getClass().getResourceAsStream(
					loggerxml));
		} catch (JoranException je) {
			// StatusPrinter will handle this
		}
		StatusPrinter.printInCaseOfErrorsOrWarnings(context);
	}
}
