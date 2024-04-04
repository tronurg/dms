package com.ogya.dms.core.database.search;

import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;

public class DmsAnalysisConfigurer implements LuceneAnalysisConfigurer {

	@Override
	public void configure(LuceneAnalysisConfigurationContext context) {
		context.analyzer("messageContentAnalyzer").custom().tokenizer(StandardTokenizerFactory.class)
				.tokenFilter(LowerCaseFilterFactory.class).tokenFilter(ASCIIFoldingFilterFactory.class);
	}

}
