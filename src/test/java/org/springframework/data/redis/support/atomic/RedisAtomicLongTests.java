/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.redis.support.atomic;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.util.Collection;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.data.redis.ConnectionFactoryTracker;
import org.springframework.data.redis.connection.ConnectionUtils;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Integration test of {@link RedisAtomicLong}
 * 
 * @author Costin Leau
 * @author Jennifer Hickey
 * @author Thomas Darimont
 */
@RunWith(Parameterized.class)
public class RedisAtomicLongTests extends AbstractRedisAtomicsTests {

	private RedisAtomicLong longCounter;
	private RedisConnectionFactory factory;

	public RedisAtomicLongTests(RedisConnectionFactory factory) {
		longCounter = new RedisAtomicLong(getClass().getSimpleName() + ":long", factory);
		this.factory = factory;
		ConnectionFactoryTracker.add(factory);
	}

	@After
	public void stop() {
		RedisConnection connection = factory.getConnection();
		connection.flushDb();
		connection.close();
	}

	@AfterClass
	public static void cleanUp() {
		ConnectionFactoryTracker.cleanUp();
	}

	@Parameters
	public static Collection<Object[]> testParams() {
		return AtomicCountersParam.testParams();
	}

	@Test
	public void testCheckAndSet() throws Exception {
		// Txs not supported in Jredis
		assumeTrue(!ConnectionUtils.isJredis(factory));
		longCounter.set(0);
		assertFalse(longCounter.compareAndSet(1, 10));
		assertTrue(longCounter.compareAndSet(0, 10));
		assertTrue(longCounter.compareAndSet(10, 0));
	}

	@Test
	public void testIncrementAndGet() throws Exception {
		longCounter.set(0);
		assertEquals(1, longCounter.incrementAndGet());
	}

	@Test
	public void testAddAndGet() throws Exception {
		longCounter.set(0);
		long delta = 5;
		assertEquals(delta, longCounter.addAndGet(delta));
	}

	@Test
	public void testDecrementAndGet() throws Exception {
		longCounter.set(1);
		assertEquals(0, longCounter.decrementAndGet());
	}

	@Test
	public void testGetExistingValue() throws Exception {
		longCounter.set(5);
		RedisAtomicLong keyCopy = new RedisAtomicLong(longCounter.getKey(), factory);
		assertEquals(longCounter.get(), keyCopy.get());
	}

	/**
	 * @see DATAREDIS-317
	 */
	@Test
	public void testShouldThrowExceptionIfAtomicLongIsUsedWithRedisTemplateAndNoKeySerializer() {

		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("a valid key serializer in template is required");

		RedisTemplate<String, Long> template = new RedisTemplate<String, Long>();
		new RedisAtomicLong("foo", template);
	}

	/**
	 * @see DATAREDIS-317
	 */
	@Test
	public void testShouldThrowExceptionIfAtomicLongIsUsedWithRedisTemplateAndNoValueSerializer() {

		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("a valid value serializer in template is required");

		RedisTemplate<String, Long> template = new RedisTemplate<String, Long>();
		template.setKeySerializer(new StringRedisSerializer());
		new RedisAtomicLong("foo", template);
	}

	/**
	 * @see DATAREDIS-317
	 */
	@Test
	public void testShouldBeAbleToUseRedisAtomicLongWithProperlyConfiguredRedisTemplate() {

		RedisTemplate<String, Long> template = new RedisTemplate<String, Long>();
		template.setConnectionFactory(factory);
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new GenericToStringSerializer<Long>(Long.class));
		template.afterPropertiesSet();

		RedisAtomicLong ral = new RedisAtomicLong("DATAREDIS-317.atomicLong", template);
		ral.set(32L);

		assertThat(ral.get(), is(32L));
	}
}
