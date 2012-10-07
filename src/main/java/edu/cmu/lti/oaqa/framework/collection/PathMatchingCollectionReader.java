package edu.cmu.lti.oaqa.framework.collection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.FileUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import edu.cmu.lti.oaqa.framework.DataElement;
import edu.cmu.lti.oaqa.framework.collection.IterableCollectionReader;

/**
 * A collection reader that can take a file (or several files that follow the same path pattern,
 * e.g., in the same directory (recursively or not). The collection reader will regard each file or
 * each line as one input element (corresponding to one CAS).
 * 
 * Required parameters: ElementUnit (LINE or FILE), PathPattern (refer to the
 * PathMatchingResourcePatternResolver in the spring framework for more detail), LINE_SYNTAX
 * (specifies the sequenceId and content parts of a line or file name, e.g., "(.*)" represents the
 * entire filename will become the sequenceId for FILE mode, and "(\w+)\s+(.*)" represents
 * sequenceId and the content are separated by whitespace(s).)
 * 
 * @author Zi Yang <ziy@cs.cmu.edu>
 * 
 */
public final class PathMatchingCollectionReader extends IterableCollectionReader {

  private static final String PATH_PATTERN_PROPERTY = "corpus-annotation.collection.path-pattern";

  private static final String ELEMENT_UNIT_PROPERTY = "corpus-annotation.collection.element-unit";

  private static final String LINE_SYNTAX_PROPERTY = "corpus-annotation.collection.line-syntax";

  public static enum ElementUnit {
    LINE, FILE
  };

  private static final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

  private Pattern lineSyntaxPattern;

  @Override
  protected Iterator<DataElement> getInputSet() throws ResourceInitializationException {
    String unit = System.getProperty(ELEMENT_UNIT_PROPERTY);
    if (unit == null) {
      System.err.printf(
              "%s property not specified, using 'ElementUnit' parameter from configuration\n",
              ELEMENT_UNIT_PROPERTY);
      unit = (String) getConfigParameterValue("ElementUnit");
    }
    String pathPattern = System.getProperty(PATH_PATTERN_PROPERTY);
    if (pathPattern == null) {
      System.err.printf(
              "%s property not specified, using 'PathPattern' parameter from configuration\n",
              PATH_PATTERN_PROPERTY);
      pathPattern = (String) getConfigParameterValue("PathPattern");
    }
    String lineSyntax = System.getProperty(PATH_PATTERN_PROPERTY);
    if (lineSyntax == null) {
      System.err.printf(
              "%s property not specified, using 'LineSyntax' parameter from configuration\n",
              LINE_SYNTAX_PROPERTY);
      lineSyntax = (String) getConfigParameterValue("LineSyntax");
    }
    lineSyntaxPattern = Pattern.compile(lineSyntax);
    try {
      Resource[] resources = resolver.getResources(pathPattern);
      switch (ElementUnit.valueOf(unit)) {
        case FILE:
          return new FileAsDataElementIterator(resources);
        case LINE:
          return new LineAsDataElementIterator(resources);
        default:
          return null;
      }
    } catch (IOException e) {
      throw new ResourceInitializationException(e);
    }
  }

  private final class FileAsDataElementIterator implements Iterator<DataElement> {

    private Resource[] resources;

    private int ptr;

    public FileAsDataElementIterator(Resource[] resources) {
      this.resources = resources;
      this.ptr = 0;
    }

    @Override
    public boolean hasNext() {
      return ptr < resources.length;
    }

    @Override
    public DataElement next() {
      try {
        InputStreamReader reader = new InputStreamReader(resources[ptr].getInputStream());
        Matcher matcher = lineSyntaxPattern.matcher(resources[ptr].getFilename());
        boolean matches = matcher.matches();
        assert matches;
        String sequenceId = matcher.group(1);
        String text = FileUtils.reader2String(reader);
        return new DataElement(getDataset(), sequenceId, text, null);
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        ptr++;
      }
      return null;
    }

    @Override
    public void remove() {
    }

  }

  private final class LineAsDataElementIterator implements Iterator<DataElement> {

    private Resource[] resources;

    private int ptr;

    private BufferedReader reader = null;

    private String line = null;

    private boolean nextCalled = true;

    public LineAsDataElementIterator(Resource[] resources) {
      this.resources = resources;
      this.ptr = -1;
    }

    @Override
    public boolean hasNext() {
      // avoid multiple calls of hasNext() without calling next() so that the pointer is keeping
      // forwarding.
      if (!nextCalled) {
        return line != null;
      }
      nextCalled = false;
      try {
        while (reader == null || (line = reader.readLine()) == null) {
          ptr++;
          if (ptr >= resources.length) {
            return false;
          }
          reader = new BufferedReader(new InputStreamReader(resources[ptr].getInputStream()));
        }
        return true;
      } catch (IOException e) {
        e.printStackTrace();
      }
      return false;
    }

    @Override
    public DataElement next() {
      if (nextCalled) {
        hasNext();
      }
      nextCalled = true;
      Matcher matcher = lineSyntaxPattern.matcher(line);
      boolean matches = matcher.matches();
      assert matches;
      return new DataElement(getDataset(), matcher.group(1), matcher.group(2), null);
    }

    @Override
    public void remove() {
    }

  }
}
