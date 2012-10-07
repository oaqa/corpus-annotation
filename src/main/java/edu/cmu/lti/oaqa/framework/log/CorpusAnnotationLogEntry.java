package edu.cmu.lti.oaqa.framework.log;

import edu.cmu.lti.oaqa.ecd.log.LogEntry;

public enum CorpusAnnotationLogEntry implements LogEntry {
  TOKENIZE, SSPLIT, POS, LEMMA, NER, PARSE, COREF
}
