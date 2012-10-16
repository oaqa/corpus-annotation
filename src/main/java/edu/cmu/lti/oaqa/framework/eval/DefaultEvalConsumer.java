package edu.cmu.lti.oaqa.framework.eval;

import org.apache.uima.jcas.tcas.Annotation;

import edu.cmu.lti.oaqa.framework.types.OutputElement;

public class DefaultEvalConsumer extends AnnotationEvalConsumer {

  public DefaultEvalConsumer() {
    super(OutputElement.type, Annotation.type);
  }

}
