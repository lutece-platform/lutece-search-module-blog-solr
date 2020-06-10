package fr.paris.lutece.plugins.blog.modules.solr.daemon;

import java.util.Date;
import java.util.List;

import fr.paris.lutece.plugins.blog.business.BlogHome;
import fr.paris.lutece.plugins.blog.business.portlet.BlogPublicationHome;
import fr.paris.lutece.plugins.blog.service.BlogService;
import fr.paris.lutece.portal.service.daemon.Daemon;

/**
 * This Daemon checks if a blog has no current publication.<br>
 * If this is the case, it will signal the solr indexer so that the blog is removed from the index.
 */
public class OutdatedBlogDaemon extends Daemon
{
    @Override
    public void run( )
    {
        List<Integer> idList = BlogHome.getIdBlogsList( );
        for ( Integer idBlog : idList )
        {
            int nbPublication = BlogPublicationHome.countPublicationByIdBlogAndDate( idBlog, new Date( ) );
            if ( nbPublication == 0 )
            {
                BlogService.getInstance( ).fireDeleteBlogEvent( idBlog );
            }
        }
    }
}
