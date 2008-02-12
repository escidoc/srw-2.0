package de.escidoc.sb.srw.lucene.sorting;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.ScoreDocComparator;
import org.apache.lucene.search.SortComparatorSource;
import org.apache.lucene.search.SortField;

/**
 * Custom sorter that sorts digits and text.
 * 
 * @author $Author$
 * @version $Revision$
 * 
 */
	public class EscidocSearchResultComparator implements SortComparatorSource, ScoreDocComparator
	{
		private IndexReader reader;
		private String fieldName;
		private boolean reverse;
		
		public EscidocSearchResultComparator ()
		{
			this( false );
		}
		
		public EscidocSearchResultComparator (boolean reverse)
		{
			this.reverse = reverse;
		}
		
		public EscidocSearchResultComparator (IndexReader reader, String fieldName)
		{
			this( reader, fieldName, false );
		}
		
		public EscidocSearchResultComparator (IndexReader reader, String fieldName, boolean reverse)
		{
			this.reader = reader;
			this.fieldName = fieldName;
			this.reverse = reverse;
		}
		
				
		public ScoreDocComparator newComparator (IndexReader reader, String fieldName)
		{
			EscidocSearchResultComparator dc = new EscidocSearchResultComparator( reader, fieldName, reverse );
			
			return dc;
		}
		
		public int compare(ScoreDoc i, ScoreDoc j) {
			try {
				Document doc1 = reader.document(i.doc);
				Document doc2 = reader.document(j.doc);
				String fieldvalue1 = doc1.get(fieldName);
				String fieldvalue2 = doc2.get(fieldName);
				if (fieldvalue1 == null && fieldvalue2 == null) {
					return 0;
				}
				if (fieldvalue1 == null && fieldvalue2 != null) {
					return -1;
				}
				if (fieldvalue1 != null && fieldvalue2 == null) {
					return 1;
				}
				return fieldvalue1.compareToIgnoreCase(fieldvalue2);
			} catch (Exception e) {
				return 0;
			}
		}

		public int sortType() {
			return SortField.CUSTOM;
		}

		public Comparable sortValue(ScoreDoc i) {
			try {
				Document doc = reader.document(i.doc);
				return doc.get(fieldName);
			} catch (Exception e) {
				return "";
			}
		}
	}
		
