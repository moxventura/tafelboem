package nl.jeeninga.tafelboem.core;

/**
 * The bombs that exist so far. Each tier decides the show and the stakes, never the sum itself.
 */
public enum BombTier {
	KNALLETJE("knalletje", 1, 1.5f, 0xFFC832, 1.0),
	GEWONE_TNT("gewone_tnt", 2, 4.0f, 0xE02828, 1.5),
	KIPPENBOM("kippenbom", 3, 3.0f, 0xFFFFFF, 1.8);

	private final String id;
	private final int tier;
	private final float blastRadius;
	private final int trailColor;
	private final double yeetPower;

	BombTier(String id, int tier, float blastRadius, int trailColor, double yeetPower) {
		this.id = id;
		this.tier = tier;
		this.blastRadius = blastRadius;
		this.trailColor = trailColor;
		this.yeetPower = yeetPower;
	}

	public String id() {
		return id;
	}

	public int tier() {
		return tier;
	}

	public float blastRadius() {
		return blastRadius;
	}

	/** The colour of the particle trail behind a thrown bomb, as 0xRRGGBB. */
	public int trailColor() {
		return trailColor;
	}

	/** How hard Graaf Fout gets yeeted when this bomb is kicked back at him. */
	public double yeetPower() {
		return yeetPower;
	}

	public static BombTier byTier(int tier) {
		for (BombTier bomb : values()) {
			if (bomb.tier == tier) {
				return bomb;
			}
		}
		throw new IllegalArgumentException("No bomb with tier " + tier);
	}
}
