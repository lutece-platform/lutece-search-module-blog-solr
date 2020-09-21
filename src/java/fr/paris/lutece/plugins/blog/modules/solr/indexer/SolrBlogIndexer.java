/*
 * Copyright (c) 2002-2020, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.blog.modules.solr.indexer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.xml.sax.ContentHandler;

import fr.paris.lutece.plugins.blog.business.Blog;
import fr.paris.lutece.plugins.blog.business.DocContent;
import fr.paris.lutece.plugins.blog.business.DocContentHome;
import fr.paris.lutece.plugins.blog.business.Tag;
import fr.paris.lutece.plugins.blog.business.portlet.BlogPublication;
import fr.paris.lutece.plugins.blog.service.BlogService;
import fr.paris.lutece.plugins.blog.utils.BlogUtils;
import fr.paris.lutece.plugins.search.solr.business.field.Field;
import fr.paris.lutece.plugins.search.solr.indexer.SolrIndexer;
import fr.paris.lutece.plugins.search.solr.indexer.SolrIndexerService;
import fr.paris.lutece.plugins.search.solr.indexer.SolrItem;
import fr.paris.lutece.plugins.search.solr.util.LuteceSolrException;
import fr.paris.lutece.plugins.search.solr.util.SolrConstants;
import fr.paris.lutece.plugins.search.solr.util.TikaIndexerUtil;
import fr.paris.lutece.portal.service.util.AppException;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.util.url.UrlItem;

/**
 * The indexer service for Solr.
 *
 */
public class SolrBlogIndexer implements SolrIndexer
{
    public static final String BEAN_NAME = "blog-solr.solrBlogIndexer";
    private static final String TYPE = "blogs";
    private static final String COMMENT = "comment";
    private static final String LABEL = "label";
    private static final String HTML_CONTENT = "htmlContent";

    private static final String PARAMETER_PORTLET_ID = "portlet_id";
    private static final String PROPERTY_INDEXER_ENABLE = "solr.indexer.document.enable";
    private static final String PROPERTY_NAME = "blog-solr.indexer.name";
    private static final String PROPERTY_DESCRIPTION = "blog-solr.indexer.description";
    private static final String PROPERTY_VERSION = "blog-solr.indexer.version";
    private static final String PARAMETER_BLOG_ID = "id";
    private static final String PARAMETER_XPAGE = "page";
    private static final String XPAGE_BLOG = "blog";
    private static final List<String> LIST_RESSOURCES_NAME = new ArrayList<>( );
    private static final String SHORT_NAME = "blog";
    private static final String DOC_INDEXATION_ERROR = "[SolrBlogIndexer] An error occured during the indexation of the document number ";
    private static final String DOC_PARSING_ERROR = "[SolrBlogIndexer] Error during document parsing. ";

    /**
     * Creates a new SolrPageIndexer
     */
    public SolrBlogIndexer( )
    {
        LIST_RESSOURCES_NAME.add( BlogUtils.CONSTANT_TYPE_RESOURCE );
    }

    @Override
    public boolean isEnable( )
    {
        return "true".equalsIgnoreCase( AppPropertiesService.getProperty( PROPERTY_INDEXER_ENABLE ) );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> indexDocuments( )
    {
        List<String> lstErrors = new ArrayList<>( );
        List<Integer> listDocument = new ArrayList<>( );

        Collection<SolrItem> solrItems = new ArrayList<>( );

        for ( Blog document : BlogService.getInstance( ).getListBlogWithoutBinaries( ) )
        {
            try
            {

                if ( !listDocument.contains( document.getId( ) ) )
                {
                    // Generates the item to index
                    SolrItem item = getItem( document );

                    if ( item != null )
                    {
                        solrItems.add( item );
                    }
                    listDocument.add( document.getId( ) );
                }
            }
            catch ( Exception e )
            {
                lstErrors.add( SolrIndexerService.buildErrorMessage( e ) );
                AppLogService.error( DOC_INDEXATION_ERROR + document.getId( ), e );
            }
        }

        if ( CollectionUtils.isNotEmpty( solrItems ) )
        {
            try
            {
                SolrIndexerService.write( solrItems );
            }
            catch ( Exception e )
            {
                lstErrors.add( SolrIndexerService.buildErrorMessage( e ) );
                AppLogService.error( DOC_INDEXATION_ERROR, e );
            }
        }
        return lstErrors;
    }

    /**
     * Index list of documents
     * 
     * @param listIdDocument
     * @return error LIST
     * @throws LuteceSolrException
     */
    public List<String> indexListDocuments( List<Integer> listIdDocument ) throws LuteceSolrException
    {
        List<String> lstErrors = new ArrayList<>( );

        Collection<SolrItem> solrItems = new ArrayList<>( );

        for ( Integer d : listIdDocument )
        {

            Blog document = BlogService.getInstance( ).findByPrimaryKeyWithoutBinaries( d );
            // Generates the item to index
            if ( document != null )
            {
                SolrItem item = getItem( document );

                if ( item != null )
                {
                    solrItems.add( item );
                }

            }
        }

        if ( CollectionUtils.isNotEmpty( solrItems ) )
        {
            try
            {
                SolrIndexerService.write( solrItems );
            }
            catch ( Exception e )
            {
                lstErrors.add( SolrIndexerService.buildErrorMessage( e ) );
                AppLogService.error( DOC_INDEXATION_ERROR, e );
                throw new LuteceSolrException( DOC_INDEXATION_ERROR, e );
            }
        }
        return lstErrors;
    }

    /**
     * Builds a document which will be used by solr during the indexing of the pages
     * of the site with the following fields : summary, uid, url, contents, title
     * and description.
     * 
     * @param document The document
     * @return The item
     */
    private SolrItem getItem( Blog document )
    {
        // Search for published blogs.
        Date today = new Date( );
        List<BlogPublication> listBlogPublications = document.getBlogPublication( ).stream( ).filter(
                bp -> bp.getDateBeginPublishing( ).before( today ) && bp.getDateEndPublishing( ).after( today ) )
                .collect( Collectors.toList( ) );

        if ( CollectionUtils.isEmpty( listBlogPublications ) )
        {
            return null;
        }

        // the item
        SolrItem item = new SolrItem( );
        item.setUid( getResourceUid( Integer.toString( document.getId( ) ), BlogUtils.CONSTANT_TYPE_RESOURCE ) );
        item.setDate( document.getUpdateDate( ) );
        item.setSummary( document.getDescription( ) );
        item.setTitle( document.getName( ) );
        item.setType( TYPE );
        item.setSite( SolrIndexerService.getWebAppName( ) );
        item.setRole( "none" );
        String portlet = listBlogPublications.stream( ).map( BlogPublication::getIdPortlet ).map( String::valueOf )
                .collect( Collectors.joining( SolrConstants.CONSTANT_AND ) );
        item.setDocPortletId( portlet );

        // Reload the full object to get all its searchable attributes
        UrlItem url = new UrlItem( SolrIndexerService.getBaseUrl( ) );
        url.addParameter( PARAMETER_XPAGE, XPAGE_BLOG );
        url.addParameter( PARAMETER_BLOG_ID, document.getId( ) );
        url.addParameter( PARAMETER_PORTLET_ID, listBlogPublications.get( 0 ).getIdPortlet( ) );
        item.setUrl( url.getUrl( ) );

        // Date Hierarchy
        GregorianCalendar calendar = new GregorianCalendar( );
        calendar.setTime( document.getUpdateDate( ) );
        item.setHieDate( calendar.get( Calendar.YEAR ) + "/" + ( calendar.get( Calendar.MONTH ) + 1 ) + "/"
                + calendar.get( Calendar.DAY_OF_MONTH ) + "/" );

        List<String> categorie = new ArrayList<>( );

        for ( Tag cat : document.getTag( ) )
        {
            categorie.add( cat.getName( ) );
        }

        item.setCategorie( categorie );

        // The content
        String strContentToIndex = getContentToIndex( document, item );
        try
        {
            ContentHandler handler =  TikaIndexerUtil.parseHtml( strContentToIndex );
            item.setContent( handler.toString( ) );
            
            List<DocContent> list = DocContentHome.getDocsContentByHtmlDoc( document.getId( ) );
            if ( CollectionUtils.isNotEmpty( list ) )
            {
                // Parse All Doc Contents
                TikaIndexerUtil.addFileContentToSolrItem( item, list.stream( ).map( DocContent::getBinaryValue ).collect( Collectors.toList( ) ) );
            }
        }
        catch ( LuteceSolrException e )
        {
            throw new AppException( DOC_PARSING_ERROR, e );
        }

        return item;
    }

    /**
     * GEt the content to index
     * 
     * @param document The document
     * @param item     The SolR item
     * @return The content
     */
    private static String getContentToIndex( Blog document, SolrItem item )
    {
        StringBuilder sbContentToIndex = new StringBuilder( );
        sbContentToIndex.append( document.getName( ) );
        sbContentToIndex.append( " " );
        sbContentToIndex.append( document.getHtmlContent( ) );
        sbContentToIndex.append( " " );
        sbContentToIndex.append( document.getDescription( ) );

        item.addDynamicField( COMMENT, document.getEditComment( ) );
        item.addDynamicField( LABEL, document.getContentLabel( ) );
        item.addDynamicField( HTML_CONTENT, document.getHtmlContent( ) );
        return sbContentToIndex.toString( );
    }

    // GETTERS & SETTERS
    /**
     * Returns the name of the indexer.
     *
     * @return the name of the indexer
     */
    @Override
    public String getName( )
    {
        return AppPropertiesService.getProperty( PROPERTY_NAME );
    }

    /**
     * Returns the version.
     *
     * @return the version.
     */
    @Override
    public String getVersion( )
    {
        return AppPropertiesService.getProperty( PROPERTY_VERSION );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription( )
    {
        return AppPropertiesService.getProperty( PROPERTY_DESCRIPTION );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Field> getAdditionalFields( )
    {
        return new ArrayList<>( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SolrItem> getDocuments( String strIdDocument )
    {
        List<SolrItem> lstItems = new ArrayList<>( );

        int nIdDocument = Integer.parseInt( strIdDocument );
        Blog document = BlogService.getInstance( ).findByPrimaryKeyWithoutBinaries( nIdDocument );

        try
        {
            SolrItem sorlItem = getItem( document );
            if ( sorlItem != null )
            {
                lstItems.add( sorlItem );
            }
        }
        catch ( Exception e )
        {
            throw new AppException( e.getMessage( ), e );
        }

        return lstItems;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getResourcesName( )
    {
        return LIST_RESSOURCES_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResourceUid( String strResourceId, String strResourceType )
    {
        StringBuilder sb = new StringBuilder( strResourceId );
        sb.append( SolrConstants.CONSTANT_UNDERSCORE ).append( SHORT_NAME );

        return sb.toString( );
    }
}
