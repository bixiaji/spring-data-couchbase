/*
 * Copyright 2013, 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.couchbase.repository.query;

import java.util.ArrayList;
import java.util.List;

import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.ViewResponse;
import com.couchbase.client.protocol.views.ViewRow;

import org.springframework.data.couchbase.core.CouchbaseOperations;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.util.StringUtils;

/**
 * Execute a repository query through the View mechanism.
 *
 * @author Michael Nitschinger
 */
public class ViewBasedCouchbaseQuery implements RepositoryQuery {

  private final CouchbaseQueryMethod method;
  private final CouchbaseOperations operations;

  public ViewBasedCouchbaseQuery(CouchbaseQueryMethod method, CouchbaseOperations operations) {
    this.method = method;
    this.operations = operations;
  }

  @Override
  public Object execute(Object[] runtimeParams) {
    Query query = null;
    Pageable pageable = null;
    for (Object param : runtimeParams) {
      if (param instanceof Query) {
        query = (Query) param;
      } else if (param instanceof Pageable) {
        pageable = (Pageable) param;
      } else {
        throw new IllegalStateException("Unknown query param: " + param);
      }
    }

    if (query == null) {
      query = new Query();
    }

    if (pageable == null) {
    	if (query.willReduce()) {
    		// with reduce, only returning count
    	    final ViewResponse response = operations.queryView(designDocName(), viewName(), query);
    	    if (response.size() == 0) {
    	    	return 0;
    	    } else {
    	    	return response.iterator().next().getValue();
    	    }
    	} else {
    		return operations.findByView(designDocName(), viewName(), query, method.getEntityInformation().getJavaType());
    	}
    } else {
        return operations.findByView(designDocName(), viewName(), query, method.getEntityInformation().getJavaType(), pageable);        
    }
  }

  @Override
  public QueryMethod getQueryMethod() {
    return method;
  }

  /**
   * Returns the best-guess design document name.
   *
   * @return the design document name.
   */
  private String designDocName() {
    if (method.hasViewAnnotation()) {
      return method.getViewAnnotation().designDocument();
    } else {
      return StringUtils.uncapitalize(method.getEntityInformation().getJavaType().getSimpleName());
    }
  }

  /**
   * Returns the best-guess view name.
   *
   * @return the view name.
   */
  private String viewName() {
    if (method.hasViewAnnotation()) {
      return method.getViewAnnotation().viewName();
    } else {
      return StringUtils.uncapitalize(method.getName().replaceFirst("find", ""));
    }
  }

}
