package net.floodlightcontroller.packet;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DNS extends BasePacket {

	private short tid;
	private short flags;
	private short questionCount;
	private short answerCount;
	private short authoriryCount = (short)0;
	private short additionCount = (short)0;
	private IPacket remains;

	private List<DNSQuery> querys = new ArrayList<DNSQuery>();
	private List<DNSAnswer> answers = new ArrayList<DNSAnswer>();

	@Override
	public byte[] serialize() {
		int length = 0;
		length += 12;
		for (short i = (short) 0; i < questionCount; i++) {
			length += querys.get(i).getLength();
		}
		for (short i = (short) 0; i < answerCount; i++) {
			length += answers.get(i).LENGTH;
		}
		if(remains != null){
			length += remains.serialize().length;
		}
		byte[] data = new byte[length];
		ByteBuffer bb = ByteBuffer.wrap(data);
		bb.putShort(getTid()).putShort(getFlags()).putShort(getQuestionCount())
				.putShort(getAnswerCount()).putShort(getAuthoriryCount())
				.putShort(additionCount);
		for (short i = (short) 0; i < questionCount; i++) {
			bb.put(querys.get(i).serialize());
		}
		for (short i = (short) 0; i < answerCount; i++) {
			bb.put(answers.get(i).serialize());
		}
		if(remains != null){
			bb.put(remains.serialize());
		}
		return data;
	}

	@Override
	public IPacket deserialize(byte[] data, int offset, int length)
			throws PacketParsingException {
		ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
		tid = bb.getShort();
		flags = bb.getShort();
		questionCount = bb.getShort();
		answerCount = bb.getShort();
		authoriryCount = bb.getShort();
		additionCount = bb.getShort();
		for (short i = (short) 0; i < questionCount; i++) {
			querys.add(new DNSQuery().deserialize(bb));
		}
		for (short i = (short) 0; i < answerCount; i++) {
			answers.add(new DNSAnswer().deserialize(bb));
		}
		setRemains(new Data().deserialize(data, bb.position(),
				bb.limit() - bb.position()));
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj){
			return true;
		}
		if (!super.equals(obj)){
			return false;
		}
		if (!(obj instanceof DNS)){
			return false;
		}
		DNS other = (DNS) obj;
		if (this.getTid() != other.getTid()){
			return false;
		}
		if (this.getFlags() != other.getFlags()){
			return false;
		}
		if (this.getQuestionCount() != other.getQuestionCount()){
			return false;
		}
		if (this.getAnswerCount() != other.getAnswerCount()){
			return false;
		}
		if (this.getAdditionCount() != other.getAdditionCount()){
			return false;
		}
		if (this.getAuthoriryCount() != other.getAuthoriryCount()){
			return false;
		}
		if (!this.getQuerys().equals(other.getQuerys())){
			return false;
		}
		if (!this.getAnswers().equals(other.getAnswers())){
			return false;
		}
		if (!this.getRemains().equals(other.getRemains())){
			return false;
		}
		return true;
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
	public int hashCode() {
		final int prime = 773;
		int result = 1;
		result += result * prime + getTid();
		result += result * prime + getFlags();
		result += result * prime + getQuestionCount();
		result += result * prime + getAnswerCount();
		result += result * prime + getAuthoriryCount();
		result += result * prime + getAdditionCount();
		result += result * prime + getAnswers().hashCode();
		result += result * prime + getQuerys().hashCode();
		result += result * prime + getRemains().hashCode();
		return result;
	}

	public short getTid() {
		return tid;
	}

	public DNS setTid(short tid) {
		this.tid = tid;
		return this;
	}

	public short getFlags() {
		return flags;
	}

	public DNS setFlags(short flags) {
		this.flags = flags;
		return this;
	}

	public short getQuestionCount() {
		return questionCount;
	}

	public DNS setQuestionCount(short questionCount) {
		this.questionCount = questionCount;
		return this;
	}

	public short getAnswerCount() {
		return answerCount;
	}

	public DNS setAnswerCount(short answerCount) {
		this.answerCount = answerCount;
		return this;
	}

	public short getAuthoriryCount() {
		return authoriryCount;
	}

	public DNS setAuthoriryCount(short authoriryCount) {
		this.authoriryCount = authoriryCount;
		return this;
	}

	public short getAdditionCount() {
		return additionCount;
	}

	public DNS setAdditionCount(short additionCount) {
		this.additionCount = additionCount;
		return this;
	}

	public List<DNSQuery> getQuerys() {
		return querys;
	}

	public DNS setQuerys(List<DNSQuery> querys) {
		this.querys = querys;
		return this;
	}

	public List<DNSAnswer> getAnswers() {
		return answers;
	}

	public DNS setAnswers(List<DNSAnswer> answers) {
		this.answers = answers;
		return this;
	}

	public IPacket getRemains() {
		return remains;
	}

	public DNS setRemains(IPacket remains) {
		this.remains = remains;
		return this;
	}
}
