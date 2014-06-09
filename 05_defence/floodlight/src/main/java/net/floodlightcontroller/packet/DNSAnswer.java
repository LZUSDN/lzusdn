package net.floodlightcontroller.packet;

import java.nio.ByteBuffer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DNSAnswer {
	private short name = (short) 0xc00c;
	private short type = (short)1;
	private short cls = (short)1;
	private int ttl = 60;
	private short addrLength = (short)4;
	private int addr;
	public final int LENGTH = 16;
	
	public byte[] serialize() {
		byte[] data = new byte[LENGTH];
		ByteBuffer bb = ByteBuffer.wrap(data);
		bb.putShort(getName())
		  .putShort(getType())
		  .putShort(getCls())
		  .putInt(getTtl())
		  .putShort(getAddrLength())
		  .putInt(getAddr());
		return data;
	}
	public DNSAnswer deserialize(ByteBuffer bb) {
		this.setName(bb.getShort())
			.setType(bb.getShort())
			.setCls(bb.getShort())
			.setTtl(bb.getInt())
			.setAddrLength(bb.getShort())
			.setAddr(bb.getInt());
		return this;
	}
	@Override
	public int hashCode() {
		final int prime = 919;
		int result = 1;
		result += result * prime + getName();
		result += result * prime + getType();
		result += result * prime + getCls();
		result += result * prime + getTtl();
		result += result * prime + getAddrLength();
		result += result * prime + getAddr();
		return result;
	}
	@Override
	public String toString(){
		ObjectMapper mapper = new ObjectMapper();
		String result = null;
		try {
			result = mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj){
			return true;
		}
		if (!super.equals(obj)){
			return false;
		}
		if (!(obj instanceof DNSAnswer)){
			return false;
		}
		DNSAnswer other = (DNSAnswer) obj;
		if (this.getName() != other.getName()){
			return false;
		}
		if (this.getType() != other.getType()){
			return false;
		}
		if (this.getCls() != other.getCls()){
			return false;
		}
		if (this.getTtl() != other.getTtl()){
			return false;
		}
		if (this.getAddrLength() != other.getAddrLength()){
			return false;
		}
		if (this.getAddr() != other.getAddr()){
			return false;
		}
		return true;
	}
	public short getName() {
		return name;
	}
	public DNSAnswer setName(short name) {
		this.name = name;
		return this;
	}
	public short getType() {
		return type;
	}
	public DNSAnswer setType(short type) {
		this.type = type;
		return this;
	}
	public short getCls() {
		return cls;
	}
	public DNSAnswer setCls(short cls) {
		this.cls = cls;
		return this;
	}
	public int getTtl() {
		return ttl;
	}
	public DNSAnswer setTtl(int ttl) {
		this.ttl = ttl;
		return this;
	}
	public short getAddrLength() {
		return addrLength;
	}
	public DNSAnswer setAddrLength(short length) {
		this.addrLength = length;
		return this;
	}
	public int getAddr() {
		return addr;
	}
	public DNSAnswer setAddr(int addr) {
		this.addr = addr;
		return this;
	}
	public DNSAnswer setAddr(String addr){
		this.setAddr(IPv4.toIPv4Address(addr));
		return this;
	}
}