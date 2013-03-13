package net.nightwhistler.pageturner.catalog;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import net.nightwhistler.htmlspanner.HtmlSpanner;
import net.nightwhistler.nucular.atom.Entry;
import net.nightwhistler.nucular.atom.Feed;
import net.nightwhistler.nucular.atom.Link;
import net.nightwhistler.pageturner.R;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class Catalog {

	/**
	 * Reserved ID to identify the feed entry where custom sites are added.
	 */
	public static final String CUSTOM_SITES_ID = "IdCustomSites";
	
    private static final int ABBREV_TEXT_LEN = 150;
	
	private static final int MAX_THUMBNAIL_WIDTH = 85;
	
	private static final Logger LOG = LoggerFactory.getLogger("Catalog");

	private Catalog() {}
	
	/**
	 * Selects the right image link for an entry, based on preference.
	 * 
	 * @param feed
	 * @param entry
	 * @return
	 */
	public static Link getImageLink(Feed feed, Entry entry) {
		Link[] linkOptions;

		if (isLeafEntry(feed)) {
			linkOptions = new Link[] { entry.getImageLink(), entry.getThumbnailLink() };
		} else {
			linkOptions = new Link[] { entry.getThumbnailLink(), entry.getImageLink() };						
		}
		
		Link imageLink = null;					
		for ( int i=0; imageLink == null && i < linkOptions.length; i++ ) {
			imageLink = linkOptions[i];
		}
		
		return imageLink;
	}
	
	/**
	 * Loads the details for the given entry into the given layout.
	 * 
	 * @param context
	 * @param layout
	 * @param entry
	 * @param imageLink
	 * @param abbreviateText
	 */
	public static void loadBookDetails(Context context, View layout, Entry entry, Link imageLink, boolean abbreviateText ) {
		
		HtmlSpanner spanner = new HtmlSpanner();
		
		TextView title = (TextView) layout.findViewById(R.id.itemTitle);
		TextView desc = (TextView) layout
				.findViewById(R.id.itemDescription);

		ImageView icon = (ImageView) layout.findViewById(R.id.itemIcon);
		loadImageLink(context, icon, imageLink, abbreviateText);

		title.setText(entry.getTitle());

		CharSequence text;
		
		if (entry.getContent() != null) {
			text = spanner.fromHtml(entry.getContent().getText());
		} else if (entry.getSummary() != null) {
			text = spanner.fromHtml(entry.getSummary());
		} else {
			text = "";
		}
		
		if (abbreviateText && text.length() > ABBREV_TEXT_LEN ) {
			text = text.subSequence(0, ABBREV_TEXT_LEN) + "…";
		}
		
		desc.setText(text);
	}
	
	public static void loadImageLink(Context context, ImageView icon, Link imageLink, boolean scaleToThumbnail ) {

		try {

			if (imageLink != null && imageLink.getBinData() != null) {
				byte[] data = imageLink.getBinData();

				Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0,
						data.length);

				if ( scaleToThumbnail && bitmap.getWidth() > MAX_THUMBNAIL_WIDTH ) {
					int newHeight = getThumbnailWidth(bitmap.getHeight(), bitmap.getWidth() );
					icon.setImageBitmap( Bitmap.createScaledBitmap(bitmap,
							MAX_THUMBNAIL_WIDTH, newHeight, true));
					bitmap.recycle();				
				} else {
					icon.setImageBitmap(bitmap);
				}
				
				return;
			} 
		} catch (OutOfMemoryError mem ) {

		}
		
		icon.setImageDrawable(context.getResources().getDrawable(
					R.drawable.unknown_cover));
		
	}
	
	public static int getThumbnailWidth( int originalHeight, int originalWidth ) {
		float factor = (float) originalHeight / (float) originalWidth;
		
		return (int) (MAX_THUMBNAIL_WIDTH * factor);
	}	

	
	public static void loadImageLink(HttpClient client, Map<String, byte[]> cache, Link imageLink,
			String baseUrl) throws IOException {

		//client.getCredentialsProvider().setCredentials(new AuthScope(null, -1),
				//new UsernamePasswordCredentials(user, password));

		if (imageLink != null) {
			String href = imageLink.getHref();

			if (cache.containsKey(href)) {
				imageLink.setBinData(cache.get(href));
			} else {

				String target = new URL(new URL(baseUrl), href).toString();

				LOG.info("Downloading image: " + target);

				HttpResponse resp = client.execute(new HttpGet(target));

				imageLink.setBinData(EntityUtils.toByteArray(resp.getEntity()));

				cache.put(href, imageLink.getBinData());
			}
		}
	}
	
	/**
	 * Checks if a feed is a leaf:
	 * 
	 * A feed is a leaf if it isn't our custom
	 * sites feed and only has 1 entry.
	 * 
	 * @param feed
	 * @return
	 */
	public static boolean isLeafEntry(Feed feed) {		
		
		return feed.getEntries().size() == 1
				&& ( feed.getId() == null
				|| ! feed.getId().equals(CUSTOM_SITES_ID));
	}
}
