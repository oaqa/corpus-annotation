package edu.cmu.lti.oaqa.framework.report;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.Resource_ImplBase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowCallbackHandler;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

import edu.cmu.lti.oaqa.framework.DataStoreImpl;
import edu.cmu.lti.oaqa.framework.report.ReportComponent;
import edu.cmu.lti.oaqa.framework.report.ReportComponentBuilder;

public class DatabaseReportComponentBuilder extends Resource_ImplBase implements
        ReportComponentBuilder {

  private String query;

  private String[] keys;

  private String[] fields;

  private String[] headers;

  private String[] formats;

  @Override
  public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException {
    this.query = ((String) aAdditionalParams.get("query"));
    this.keys = ((String) aAdditionalParams.get("keys")).split(",");
    this.fields = ((String) aAdditionalParams.get("fields")).split(",");
    this.formats = ((String) aAdditionalParams.get("formats")).split(",");
    this.headers = ((String) aAdditionalParams.get("headers")).split(",");
    if (fields.length != formats.length) {
      throw new ResourceInitializationException(
              "Fields and types fields lengths must agree: {0} != {1}", new Object[] {
                  fields.length,
                  formats.length });
    }
    return true;
  }

  @Override
  public ReportComponent getReportComponent(final String... args) {
    JdbcTemplate template = DataStoreImpl.getInstance().jdbcTemplate();
    final ImmutableTable.Builder<String, String, String> builder = new ImmutableTable.Builder<String, String, String>();
    final Joiner joiner = Joiner.on("/");
    RowCallbackHandler handler = new RowCallbackHandler() {
      public void processRow(ResultSet rs) throws SQLException {
        List<String> keyPath = Lists.newArrayList();
        for (String key : keys) {
          keyPath.add(rs.getString(key));
        }
        String rowId = joiner.join(keyPath);
        for (int i = 0; i < fields.length; i++) {
          Object o = rs.getObject(fields[i]);
          builder.put(rowId, headers[i], String.format(formats[i], o));
        }
      }
    };
    PreparedStatementSetter pss = new PreparedStatementSetter() {
      public void setValues(PreparedStatement ps) throws SQLException {
        for (int i = 0; i < args.length; i++) {
          ps.setString(i + 1, args[i]);
        }
      }
    };
    template.query(query, pss, handler);
    return new ReportComponent() {
      @Override
      public Table<String, String, String> getTable() {
        return builder.build();
      }

      @Override
      public List<String> getHeaders() {
        return Arrays.asList(headers);
      }
    };
  }
}
