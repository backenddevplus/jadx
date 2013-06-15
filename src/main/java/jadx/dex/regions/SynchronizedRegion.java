package jadx.dex.regions;

import jadx.dex.nodes.IContainer;
import jadx.dex.nodes.IRegion;
import jadx.dex.nodes.InsnNode;

import java.util.List;

public final class SynchronizedRegion extends AbstractRegion {

	private final InsnNode insn;
	private final Region region;

	public SynchronizedRegion(IRegion parent, InsnNode insn) {
		super(parent);
		this.insn = insn;
		this.region = new Region(this);
	}

	public InsnNode getInsn() {
		return insn;
	}

	public Region getRegion() {
		return region;
	}

	@Override
	public List<IContainer> getSubBlocks() {
		return region.getSubBlocks();
	}

	@Override
	public String toString() {
		return "Synchronized:" + region;
	}
}
