package err.sfp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Thumbnail;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Err on 16-12-12.
 */

//TODO save images to drawable, for backup and save memory
public class YoutubeClient  extends  Thread implements Consts{

    boolean done = false;
    static ArrayList<ResultItem> items = new ArrayList<>();
    //private ProgressDialog pd;
    private Download download;
    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private  YouTube youtube;
    String query;
    public YoutubeClient(Download download, String query) {
        Log.i(T, "YoutubeClient init, search");
        this.query = query;
        this.download = download;
        //pd = new ProgressDialog(context);
    }

    @Override
    public void run() {
        search(query);
    }

    public void search(String queryTerm) {
        // Read the developer key from the properties file.
        /*Properties properties = new Properties();
        try {
            InputStream in = YouTube.Search.class.getResourceAsStream("/" + PROPERTIES_FILENAME);
            properties.load(in);

        } catch (IOException e) {
            System.err.println("There was an error reading " + PROPERTIES_FILENAME + ": " + e.getCause()
                    + " : " + e.getMessage());
            System.exit(1);
        }*/

        try {
            /*pd.show(context, context.getString(R.string.SEARCH_TITLE), context.getString(R.string.SEARCHING)
                     , true, false);*/
            // This object is used to make YouTube Data API requests. The last
            // argument is required, but since we don't need anything
            // initialized when the HttpRequest is initialized, we override
            // the interface and provide a no-op function.
            youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
                public void initialize(HttpRequest request) throws IOException {
                }
            }).setApplicationName("sfp").build();


            // Define the API request for retrieving search results.
            YouTube.Search.List search = youtube.search().list("id,snippet");

            // Set your developer key from the {{ Google Cloud Console }} for
            // non-authenticated requests. See:
            // {{ https://cloud.google.com/console }}
           // String apiKey = properties.getProperty("youtube.apikey");
            search.setKey(YOUTUBE_CLIENT_ID);
            search.setQ(queryTerm);

            // Restrict the search results to only include videos. See:
            // https://developers.google.com/youtube/v3/docs/search/list#type
            search.setType("video");

            // To increase efficiency, only retrieve the fields that the
            // application uses.
            search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)");
            search.setMaxResults((long)NUMBER_OF_VIDEOS_RETURNED);

            // Call the API and print results.
            Log.i(T, "searching youtube for queryText "+queryTerm);
            SearchListResponse searchResponse = search.execute();
            List<SearchResult> searchResultList = searchResponse.getItems();
            if (searchResultList != null) {
                Log.i(T, "extracting results");
                extractResults(searchResultList.iterator());
            }


        } catch (GoogleJsonResponseException e) {
            System.err.println("There was a service error: " + e.getDetails().getCode() + " : "
                    + e.getDetails().getMessage());
        } catch (IOException e) {
            System.err.println("There was an IO error: " + e.getCause() + " : " + e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            if(items.size()>0) {
                download.handler.sendEmptyMessage(0);
            }
            done = true;
            //pd.dismiss();
        }
    }

    private void extractResults(Iterator<SearchResult> iteratorSearchResults) {

        // how view thumbnail as picture.
        if (!iteratorSearchResults.hasNext()) {
            Log.i(T, "no result");
            download.handler.sendEmptyMessage(-1);
        }
        while (iteratorSearchResults.hasNext()) {
            Log.i(T, "result processing Result video");
            SearchResult singleVideo = iteratorSearchResults.next();
            ResourceId rId = singleVideo.getId();

            // Confirm that the result represents a video. Otherwise, the
            // item will not contain a video ID.
            if (rId.getKind().equals("youtube#video")) {
                Thumbnail thumbnail = singleVideo.getSnippet().getThumbnails().getDefault();
                Log.i(T, "YoutubeClient, inserting new item into items index: "+items.size());
                items.add(new ResultItem(singleVideo.getSnippet().getTitle(),
                            getThumbnailBitmap(thumbnail.getUrl()),
                            rId.getVideoId()));
            }
        }

    }


    public Bitmap getThumbnailBitmap(String url) {
        try {
            Log.i(T, "downloading bitmap of url: "+url);
            return BitmapFactory.decodeStream(new URL(url).openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    class ResultItem {
        String title;
        Bitmap thumbnail;
        String videoId;

        public ResultItem(String title, Bitmap thumbnail, String videoId) {
            this.title = title;
            this.thumbnail = thumbnail;
            this.videoId = videoId;
        }
    }
}
