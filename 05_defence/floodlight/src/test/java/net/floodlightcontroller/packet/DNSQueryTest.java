package net.floodlightcontroller.packet;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.Test;

public class DNSQueryTest {

	byte[] pkt = new byte[] { 0x06, 0x73, 0x74, 0x61, 0x74, 0x69, 0x63,
			0x06, 0x66, 0x61, 0x6e, 0x66, 0x6f, 0x75, 0x03, 0x63, 0x6f,
			0x6d, 0x00, 0x00, 0x01, 0x00, 0x01, };
	@Test
	public void testSerialize() {
		DNSQuery query = new DNSQuery();
		query.setCls((short)1);
		query.setType((short)1);
		query.setName("static.fanfou.com");
		assertArrayEquals(query.serialize(), pkt);
	}

	@Test
	public void testDeserialize() {
		ByteBuffer bb = ByteBuffer.wrap(pkt);
		DNSQuery query = new DNSQuery().deserialize(bb);
		assertEquals(query.getName(), "static.fanfou.com");
		assertEquals(query.getCls(), (short) 0x0001);
		assertEquals(query.getType(), (short) 0x0001);
	}
}
