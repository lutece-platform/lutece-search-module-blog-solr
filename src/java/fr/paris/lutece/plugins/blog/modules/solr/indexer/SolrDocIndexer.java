package fr.paris.lutece.plugins.blog.modules.solr.indexer;

import fr.paris.lutece.plugins.blog.business.Blog;
import fr.paris.lutece.plugins.blog.business.Tag;
import fr.paris.lutece.plugins.blog.business.portlet.BlogPublication;
import fr.paris.lutece.plugins.blog.service.BlogService;
import fr.paris.lutece.plugins.blog.utils.BlogUtils;
import fr.paris.lutece.plugins.search.solr.business.field.Field;
import fr.paris.lutece.plugins.search.solr.indexer.SolrIndexer;
import fr.paris.lutece.plugins.search.solr.indexer.SolrIndexerService;
import fr.paris.lutece.plugins.search.solr.indexer.SolrItem;
import fr.paris.lutece.plugins.search.solr.util.SolrConstants;
import fr.paris.lutece.portal.business.page.Page;
import fr.paris.lutece.portal.business.page.PageHome;
import fr.paris.lutece.portal.business.portlet.Portlet;
import fr.paris.lutece.portal.service.util.AppException;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.util.url.UrlItem;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * The indexer service for Solr.
 *
 */
public class SolrDocIndexer implements SolrIndexer
{
    public static final String BEAN_NAME = "blog-solr.solrDocIndexer";
    // Not used
    // private static final String PARAMETER_SOLR_DOCUMENT_ID = "solr_document_id";
    private static final String TYPE = "blogs";
    private static final String COMMENT = "comment";
    private static final String LABEL = "label";
    private static final String HTML_CONTENT = "htmlContent";

    private static final String PARAMETER_PORTLET_ID = "portlet_id";
    private static final String PROPERTY_INDEXER_ENABLE = "solr.indexer.document.enable";
    private static final String PROPERTY_DOCUMENT_MAX_CHARS = "blog-solr.indexer.document.characters.limit";
    private static final String PROPERTY_NAME = "blog-solr.indexer.name";
    private static final String PROPERTY_DESCRIPTION = "blog-solr.indexer.description";
    private static final String PROPERTY_VERSION = "blog-solr.indexer.version";
    private static final String PARAMETER_BLOG_ID = "id";
    private static final String PARAMETER_XPAGE = "page";
    private static final String XPAGE_BLOG = "blog";
    private static final List<String> LIST_RESSOURCES_NAME = new ArrayList<String>( );
    private static final String SHORT_NAME = "blog";
    private static final String DOC_INDEXATION_ERROR = "[SolrBlogIndexer] An error occured during the indexation of the document number ";

    private static final Integer PARAMETER_DOCUMENT_MAX_CHARS = Integer.parseInt( AppPropertiesService.getProperty( PROPERTY_DOCUMENT_MAX_CHARS ) );

    /**
     * Creates a new SolrPageIndexer
     */
    public SolrDocIndexer( )
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
        List<String> lstErrors = new ArrayList<String>( );
        List<Integer> listDocument = new ArrayList<Integer>( );

        Collection<SolrItem> solrItems = new ArrayList<SolrItem>( );

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
            catch( Exception e )
            {
                lstErrors.add( SolrIndexerService.buildErrorMessage( e ) );
                AppLogService.error( DOC_INDEXATION_ERROR + document.getId( ), e );

            }
        }

        try
        {
            SolrIndexerService.write( solrItems );
        }
        catch( Exception e )
        {
            lstErrors.add( SolrIndexerService.buildErrorMessage( e ) );
            AppLogService.error( DOC_INDEXATION_ERROR, e );
        }

        return lstErrors;
    }

    /**
     * iNDEX LIST oF DICUMENT
     * 
     * @param listIdDocument
     * @return error LIST
     * @throws Exception
     */
    public List<String> indexListDocuments( Portlet portlet, List<Integer> listIdDocument ) throws Exception
    {
        List<String> lstErrors = new ArrayList<String>( );

        Collection<SolrItem> solrItems = new ArrayList<SolrItem>( );

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

        try
        {
            SolrIndexerService.write( solrItems );
        }
        catch( Exception e )
        {
            lstErrors.add( SolrIndexerService.buildErrorMessage( e ) );
            AppLogService.error( DOC_INDEXATION_ERROR, e );
            throw new Exception( );
        }

        return lstErrors;
    }

    /**
     * Get item
     * 
     * @param portlet
     *            The portlet
     * @param document
     *            The document
     * @return The item
     * @throws IOException
     */
    private SolrItem getItem( Blog document ) throws IOException
    {
        // the item
        SolrItem item = new SolrItem( );
        item.setUid( getResourceUid( Integer.valueOf( document.getId( ) ).toString( ), BlogUtils.CONSTANT_TYPE_RESOURCE ) );
        item.setDate( document.getUpdateDate( ) );
        item.setSummary( document.getDescription( ) );
        item.setTitle( document.getName( ) );
        item.setType( TYPE );
        item.setSite( SolrIndexerService.getWebAppName( ) );
        item.setRole( "none" );
        String portlet = new String( String.valueOf( document.getId( ) ) );
        List<BlogPublication> listBlogPublications = document.getBlogPubilcation( );
        for ( BlogPublication p : listBlogPublications )
        {
            portlet = SolrConstants.CONSTANT_AND + p.getIdPortlet( );
        }
        item.setDocPortletId( portlet );

        // item.setXmlContent( document.getXmlValidatedContent( ) );

        // Reload the full object to get all its searchable attributes
        UrlItem url = new UrlItem( SolrIndexerService.getBaseUrl( ) );
        url.addParameter( PARAMETER_XPAGE, XPAGE_BLOG );
        url.addParameter( PARAMETER_BLOG_ID, document.getId( ) );
        if ( listBlogPublications.size( ) > 0 )
        {
            url.addParameter( PARAMETER_PORTLET_ID, listBlogPublications.get( 0 ).getIdPortlet( ) );
        }
        item.setUrl( url.getUrl( ) );

        // Date Hierarchy
        GregorianCalendar calendar = new GregorianCalendar( );
        calendar.setTime( document.getUpdateDate( ) );
        item.setHieDate( calendar.get( GregorianCalendar.YEAR ) + "/" + ( calendar.get( GregorianCalendar.MONTH ) + 1 ) + "/"
                + calendar.get( GregorianCalendar.DAY_OF_MONTH ) + "/" );

        List<String> categorie = new ArrayList<String>( );

        for ( Tag cat : document.getTag( ) )
        {
            categorie.add( cat.getName( ) );
        }

        item.setCategorie( categorie );

        // The content
        String strContentToIndex = getContentToIndex( document, item );
        ContentHandler handler = null;
        if ( PARAMETER_DOCUMENT_MAX_CHARS != null )
        {
            handler = new BodyContentHandler( PARAMETER_DOCUMENT_MAX_CHARS );
        }
        else
        {
            handler = new BodyContentHandler( );
        }

        Metadata metadata = new Metadata( );

        try
        {
            new HtmlParser( ).parse( new ByteArrayInputStream( strContentToIndex.getBytes( ) ), handler, metadata, new ParseContext( ) );
        }
        catch( SAXException e )
        {
            throw new AppException( "Error during document parsing." );
        }
        catch( TikaException e )
        {
            throw new AppException( "Error during document parsing." );
        }

        item.setContent( handler.toString( ) );

        return item;
    }

    /**
     * GEt the content to index
     * 
     * @param document
     *            The document
     * @param item
     *            The SolR item
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
        for ( BlogPublication p : document.getBlogPubilcation( ) )
        {
            item.addDynamicField( "start_publication_portlet" + p.getIdPortlet( ), p.getDateBeginPublishing( ) );
            item.addDynamicField( "end_publication_portlet" + p.getIdPortlet( ), p.getDateEndPublishing( ) );
            item.addDynamicField( "status_portlet" + p.getIdPortlet( ), String.valueOf( p.getStatus( ) ) );

        }

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
        List<Field> lstFields = new ArrayList<Field>( );

        return lstFields;
    }

    /**
     * Builds a document which will be used by solr during the indexing of the pages of the site with the following fields : summary, uid, url, contents, title
     * and description.
     *
     * @param document
     *            the document to index
     * @param strUrl
     *            the url of the documents
     * @param strRole
     *            the lutece role of the page associate to the document
     * @param strPortletDocumentId
     *            the document id concatened to the id portlet with a & in the middle
     * @return the built Document
     * @throws IOException
     *             The IO Exception
     * @throws InterruptedException
     *             The InterruptedException
     */
    private SolrItem getDocument( Blog document, String strUrl, String strRole, String strPortletDocumentId ) throws IOException, InterruptedException
    {
        // make a new, empty document
        SolrItem item = new SolrItem( );

        // Add the url as a field named "url". Use an UnIndexed field, so
        // that the url is just stored with the document, but is not searchable.
        item.setUrl( strUrl );

        // Add the PortletDocumentId as a field named "document_portlet_id".
        item.setDocPortletId( strPortletDocumentId );

        // Add the last modified date of the file a field named "modified".
        // Use a field that is indexed (i.e. searchable), but don't tokenize
        // the field into words.
        item.setDate( document.getUpdateDate( ) );

        // Add the uid as a field, so that index can be incrementally maintained.
        // This field is not stored with document, it is indexed, but it is not
        // tokenized prior to indexing.
        String strIdDocument = String.valueOf( document.getId( ) );
        item.setUid( getResourceUid( strIdDocument, BlogUtils.CONSTANT_TYPE_RESOURCE ) );

        String strContentToIndex = getContentToIndex( document, item );
        ContentHandler handler = new BodyContentHandler( );
        Metadata metadata = new Metadata( );

        try
        {
            new org.apache.tika.parser.html.HtmlParser( ).parse( new ByteArrayInputStream( strContentToIndex.getBytes( ) ), handler, metadata,
                    new ParseContext( ) );
        }
        catch( SAXException e )
        {
            throw new AppException( "Error during document parsing." );
        }
        catch( TikaException e )
        {
            throw new AppException( "Error during document parsing." );
        }

        // Add the tag-stripped contents as a Reader-valued Text field so it will
        // get tokenized and indexed.
        item.setContent( handler.toString( ) );

        // Add the title as a separate Text field, so that it can be searched
        // separately.
        item.setTitle( document.getName( ) );

        item.setType( TYPE );

        item.setRole( strRole );

        item.setSite( SolrIndexerService.getWebAppName( ) );

        // return the document
        return item;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SolrItem> getDocuments( String strIdDocument )
    {
        List<SolrItem> lstItems = new ArrayList<SolrItem>( );

        int nIdDocument = Integer.parseInt( strIdDocument );
        Blog document = BlogService.getInstance( ).findByPrimaryKeyWithoutBinaries( nIdDocument );
        List<BlogPublication> it = document.getBlogPubilcation( );

        try
        {
            for ( BlogPublication p : it )
            {
                UrlItem url = new UrlItem( SolrIndexerService.getBaseUrl( ) );
                url.addParameter( PARAMETER_XPAGE, XPAGE_BLOG );
                url.addParameter( PARAMETER_BLOG_ID, nIdDocument );
                url.addParameter( PARAMETER_PORTLET_ID, p.getIdPortlet( ) );

                String strPortletDocumentId = nIdDocument + "&" + p.getIdPortlet( );
                Page page = PageHome.getPage( p.getPortlet( ).getPageId( ) );

                lstItems.add( getDocument( document, url.getUrl( ), page.getRole( ), strPortletDocumentId ) );
            }
        }
        catch( Exception e )
        {
            throw new RuntimeException( e );
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
