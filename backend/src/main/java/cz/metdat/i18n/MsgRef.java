package cz.metdat.i18n;

/** A message argument that is itself a message key and is translated before being inserted (e.g. an enum label). */
public record MsgRef(String key) {}
