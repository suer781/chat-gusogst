package com.gusogst.chat.agent;

/**
 * Java interface for Python → Kotlin streaming callbacks via Chaquopy.
 *
 * Chaquopy can pass Java objects to Python.  Python code calls
 * {@code onDelta(text)} on this interface instance, and Chaquopy
 * automatically marshals the call to the Kotlin/Java implementation.
 *
 * Must be a Java interface (not Kotlin) because Chaquopy's proxy
 * generation relies on standard Java reflection.  Kotlin interfaces
 * compiled to JVM bytecode also work, but a plain Java interface is
 * the simplest and most reliable.
 */
public interface StreamCallback {
    /**
     * Called for each text delta during streaming.
     *
     * @param text  A chunk of text from the LLM.  Never null.
     */
    void onDelta(String text);
}
