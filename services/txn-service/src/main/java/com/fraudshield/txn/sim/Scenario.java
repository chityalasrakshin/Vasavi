package com.fraudshield.txn.sim;

/** Ground-truth label attached to synthetic traffic. */
public enum Scenario {
    /** Ordinary payments, including legitimately unusual ones such as paying rent to a new landlord. */
    LEGIT,
    /** Victims pay a ring of new "mule" accounts that forward the money to a collector within minutes. */
    MULE_RING,
    /** A fraudster on a new device, in another city, drains the account to new payees. */
    ACCOUNT_TAKEOVER,
    /** A senior citizen is coerced by fake "officials" into escalating transfers to a new payee. */
    DIGITAL_ARREST,
    /** A newly created account fires ₹1 payments at many strangers to test which UPI IDs are live. */
    UPI_PROBING,
    /** A second-wave adversary changes amounts, timing and payment paths to evade fixed rules. */
    ADAPTIVE_FRAUD,
    /** A first-payment scam involving a brand-new payer, device and payee. */
    COLD_START_SCAM
}
