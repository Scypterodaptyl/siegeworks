package me.mss1r.siegeworks.api;

/** Attack range used when AI drives a ram or another melee siege engine. */
public interface SiegeMeleeControl extends SiegeEngineControl {
    double getAutomatedAttackRange();
}
