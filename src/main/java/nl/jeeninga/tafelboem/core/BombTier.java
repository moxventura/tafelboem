package nl.jeeninga.tafelboem.core;

/**
 * The bombs that exist so far. Each tier decides the show and the stakes, never the sum itself.
 */
public enum BombTier {
	KNALLETJE("knalletje", 1, 1.5f),
	GEWONE_TNT("gewone_tnt", 2, 4.0f),
	KIPPENBOM("kippenbom", 3, 3.0f);

	private final String id;
	private final int tier;
	private final float blastRadius;

	BombTier(String id, int tier, float blastRadius) {
		this.id = id;
		this.tier = tier;
		this.blastRadius = blastRadius;
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

	public static BombTier byTier(int tier) {
		for (BombTier bomb : values()) {
			if (bomb.tier == tier) {
				return bomb;
			}
		}
		throw new IllegalArgumentException("No bomb with tier " + tier);
	}
}
