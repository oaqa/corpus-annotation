package edu.cmu.lti.oaqa.framework.collection;

import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import edu.cmu.lti.oaqa.ecd.BaseExperimentBuilder;
import edu.cmu.lti.oaqa.framework.CasUtils;
import edu.cmu.lti.oaqa.framework.ViewManager;
import edu.cmu.lti.oaqa.framework.ViewManager.ViewType;
import edu.cmu.lti.oaqa.framework.types.InputElement;

public class DefaultGoldStandardDecorator extends JCasAnnotator_ImplBase {

  private GoldStandardPersistenceProvider persistence;

  @Override
  public void initialize(UimaContext c) throws ResourceInitializationException {
    String pp = (String) c.getConfigParameterValue("persistence-provider");
    if (pp == null) {
      throw new ResourceInitializationException(new IllegalArgumentException(
              "Must provide a parameter of type <persistence-provider>"));
    }
    this.persistence = BaseExperimentBuilder
            .loadProvider(pp, GoldStandardPersistenceProvider.class);
  }

  /**
   * Retrieves the gold standard data from the database for the retrieval task. And stores FSArray
   * of Search on the DOCUMENT_GS view of the JCas.
   */
  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    try {
      final InputElement input = (InputElement) CasUtils.getFirst(aJCas,
              InputElement.class.getName());
      final JCas gsView = ViewManager.getOrCreateView(aJCas, ViewType.DOCUMENT_GS);
      final String dataset = input.getDataset();
      final String sequenceId = input.getSequenceId();
      List<Annotation> gsAnnotations = persistence
              .populateGoldStandard(dataset, sequenceId, gsView);
      if (!gsAnnotations.isEmpty()) {
        FSArray gsList = new FSArray(gsView, gsAnnotations.size());
        for (int i = 0; i < gsAnnotations.size(); i++) {
          gsList.set(i, gsAnnotations.get(i));
        }
        gsList.addToIndexes();
      }
    } catch (Exception e) {
      throw new AnalysisEngineProcessException(e);
    }
  }

}
