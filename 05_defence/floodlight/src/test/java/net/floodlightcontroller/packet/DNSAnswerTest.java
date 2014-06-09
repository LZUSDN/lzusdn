package net.floodlightcontroller.packet;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.Test;

public class DNSAnswerTest {
	byte[] pkt = new byte[] { (byte) 0xc0, 0x0c, 0x00, 0x01, 0x00, 0x01, 0x00,
			0x00, 0x07, 0x08, 0x00, 0x04, 0x3a, 0x53, (byte) 0x81, 0x0a };

	@Test
	public void testSerialize() {
		DNSAnswer answer = new DNSAnswer();
		answer.deserialize(ByteBuffer.wrap(pkt));
		assertArrayEquals(pkt, answer.serialize());
	}

	@Test
	public void testDeserialize() {
		DNSAnswer answer = new DNSAnswer();
		answer.deserialize(ByteBuffer.wrap(pkt));
		assertEquals("name", (short)0xc00c, answer.getName());
		assertEquals("type", (short)0x0001, answer.getType());
		assertEquals("class", (short)0x0001, answer.getCls());
		assertEquals("ttl", (int)0x0708, answer.getTtl());
		assertEquals("addlen", (short)0x0004, answer.getAddrLength());
		assertEquals("add", (int)0x3a53810a, answer.getAddr());
	}

}
