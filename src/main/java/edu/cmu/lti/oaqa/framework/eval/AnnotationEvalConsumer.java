package edu.cmu.lti.oaqa.framework.eval;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import com.google.common.base.Function;
import com.google.common.collect.Ordering;

import edu.cmu.lti.oaqa.framework.ViewManager;
import edu.cmu.lti.oaqa.framework.ViewManager.ViewType;
import edu.cmu.lti.oaqa.framework.eval.retrieval.RetrievalEvalConsumer;

public class AnnotationEvalConsumer extends RetrievalEvalConsumer<Annotation> {

  private int resultType;

  private int goldStandardType;

  public AnnotationEvalConsumer(int resultType, int goldStandardType) {
    this.resultType = resultType;
    this.goldStandardType = goldStandardType;
  }

  @Override
  protected Ordering<Annotation> getOrdering() {
    return new Ordering<Annotation>() {

      @Override
      public int compare(@Nullable
      Annotation left, @Nullable
      Annotation right) {
        if (left.getBegin() != right.getBegin()) {
          return Double.compare(left.getBegin(), right.getBegin());
        } else {
          return Double.compare(right.getEnd(), left.getEnd());
        }
      }

    }.reverse();
  }

  @Override
  protected Function<Annotation, String> getToIdStringFct() {
    return new Function<Annotation, String>() {

      @Override
      @Nullable
      public String apply(@Nullable
      Annotation input) {
        return input.getBegin() + "-" + input.getEnd();
      }
    };
  }

  @Override
  protected List<Annotation> getGoldStandard(JCas jcas) throws CASException {
    return getAnnotations(ViewManager.getOrCreateView(jcas, ViewType.CANDIDATE_GS),
            goldStandardType);
  }

  @Override
  protected List<Annotation> getResults(JCas jcas) throws CASException {
    return getAnnotations(ViewManager.getOrCreateView(jcas, ViewType.CANDIDATE), resultType);
  }

  private static List<Annotation> getAnnotations(JCas jcas, int type) {
    List<Annotation> annotations = new ArrayList<Annotation>();
    for (Annotation annotation : jcas.getAnnotationIndex(type)) {
      if (annotation.getTypeIndexID() != type) {
        continue;
      }
      annotations.add(annotation);
    }
    return annotations;
  }

}
