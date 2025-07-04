package com.psia.pkoc;

/**
 * PKOC connection type
 */
public enum PKOC_ConnectionType
{
    /**
     * Standard flow
     */
    Uncompressed,
    /**
     * ECDHE with perfect forward secrecy flow
     */
    ECHDE_Full,
}
