package net.floodlightcontroller.packet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DNSTest {
	byte[] pkt = new byte[] { 0x44, (byte) 0xef, ((byte) 0x81), (byte) 0x80,
			0x00, 0x01, 0x00, 0x01, 0x00, 0x02, 0x00, 0x02, 0x07, 0x61, 0x76,
			0x61, 0x74, 0x61, 0x72, 0x31, 0x06, 0x66, 0x61, 0x6e, 0x66, 0x6f,
			0x75, 0x03, 0x63, 0x6f, 0x6d, 0x00, 0x00, 0x01, 0x00, 0x01,
			(byte) 0xc0, 0x0c, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x07, 0x08,
			0x00, 0x04, 0x3a, 0x53, (byte) 0x81, 0x0a, (byte) 0xc0, 0x14, 0x00,
			0x02, 0x00, 0x01, 0x00, 0x00, 0x02, (byte) 0xbc, 0x00, 0x06, 0x03,
			0x6e, 0x73, 0x32, (byte) 0xc0, 0x14, (byte) 0xc0, 0x14, 0x00, 0x02,
			0x00, 0x01, 0x00, 0x00, 0x02, (byte) 0xbc, 0x00, 0x06, 0x03, 0x6e,
			0x73, 0x31, (byte) 0xc0, 0x14, (byte) 0xc0, 0x52, 0x00, 0x01, 0x00,
			0x01, 0x00, 0x00, 0x02, (byte) 0xbc, 0x00, 0x04, 0x3a, 0x53,
			(byte) 0x81, 0x12, (byte) 0xc0, 0x40, 0x00, 0x01, 0x00, 0x01, 0x00,
			0x00, 0x02, (byte) 0xbc, 0x00, 0x04, 0x3a, 0x53, (byte) 0x81, 0x11 };

	@Test
	public void testSerialize() {
		DNS dns = new DNS();
		try {
			dns.deserialize(pkt, 0, pkt.length);
		} catch (PacketParsingException e) {
			e.printStackTrace();
		}
		assertArrayEquals(pkt, dns.serialize());
	}

	@Test
	public void testDeserialize() {
		DNS dns = new DNS();
		try {
			dns.deserialize(pkt, 0, pkt.length);
		} catch (PacketParsingException e) {
			e.printStackTrace();
		}
		assertEquals((short)0x44ef, dns.getTid());
		assertEquals((short)0x8180, dns.getFlags());
		assertEquals((short)0x0001, dns.getQuestionCount());
		assertEquals((short)0x0001, dns.getAnswerCount());
		assertEquals((short)0x0002, dns.getAuthoriryCount());
		assertEquals((short)0x0002, dns.getAdditionCount());
	}

}
