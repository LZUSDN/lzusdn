package net.floodlightcontroller.packet;

import java.nio.ByteBuffer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class DNSQuery {
	private String name;
	private short type = (short)1;
	private short cls = (short)1;
	private final int EXTRA = 6;
	private int length = 0;
	
	public byte[] serialize() {
		length += getName().length();
		length += EXTRA;
		byte[] data = new byte[length];
		ByteBuffer bb = ByteBuffer.wrap(data);
		serializeName(bb);
		bb.putShort(getType());
		bb.putShort(getCls());
		return data;
	}

	private void serializeName(ByteBuffer bb){
		String[] nameParts = getName().split("\\.");
		for(int i=0; i<nameParts.length; i++){
			bb.put((byte)nameParts[i].length());
			for(int j=0; j<nameParts[i].length(); j++){
				bb.put((byte)nameParts[i].charAt(j));
			}
		}
		bb.put((byte)0);
	}
	
	public DNSQuery deserialize(ByteBuffer bb){
		byte count;
		StringBuilder sb = new StringBuilder();
		while(true){
			count = bb.get();
			if(count != (short)0){
				for(byte i = (byte)0; i<count; i++){
					sb.append((char)bb.get());
				}
				sb.append('.');
			} else {
				break;
			}
		}
		sb.deleteCharAt(sb.length()-1);
		setName(sb.toString());
		setType(bb.getShort());
		setCls(bb.getShort());
		return this;
	}
	@Override
	public int hashCode() {
		final int prime = 569;
		int result = 1;
		result += result * prime + getName().hashCode();
		result += result * prime + getType();
		result += result * prime + getCls();
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
		if (!(obj instanceof DNSQuery)){
			return false;
		}
		DNSQuery other = (DNSQuery) obj;
		if (!this.getName().equals(other.getName())){
			return false;
		}
		if (this.getType() != other.getType()){
			return false;
		}
		if (this.getCls() != other.getCls()){
			return false;
		}
		return true;
	}
	public String getName() {
		return name;
	}
	public DNSQuery setName(String name) {
		this.name = name;
		return this;
	}
	public short getType() {
		return type;
	}
	public DNSQuery setType(short type) {
		this.type = type;
		return this;
	}
	public short getCls() {
		return cls;
	}
	public DNSQuery setCls(short cls) {
		this.cls = cls;
		return this;
	}

	public int getLength() {
		return getName().length() + EXTRA;
	}
}