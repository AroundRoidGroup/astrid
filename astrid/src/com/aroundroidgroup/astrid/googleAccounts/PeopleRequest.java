package com.aroundroidgroup.astrid.googleAccounts;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/***
 * preapering, sending and receiving the server's requests and response
 * @author Tomer
 *
 */
public class PeopleRequest {

    /***
     * creating name value pairs for server request
     * @param myFp contains gps status. may be null
     * @param people array of people to be requested
     * @return kist of the name value pairs
     */
    private static List<NameValuePair> createPostData(FriendProps myFp,String[] people){
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
        if (myFp!=null && myFp.isValid()){
            nameValuePairs.add(new BasicNameValuePair("GPSLAT", myFp.getLat())); //$NON-NLS-1$
            nameValuePairs.add(new BasicNameValuePair("GPSLON", myFp.getLon())); //$NON-NLS-1$
            nameValuePairs.add(new BasicNameValuePair("TIMESTAMP",myFp.getTime())); //$NON-NLS-1$
        }
        else{
            nameValuePairs.add(new BasicNameValuePair("GPSLAT", String.valueOf(0.0))); //$NON-NLS-1$
            nameValuePairs.add(new BasicNameValuePair("GPSLON", String.valueOf(0.0))); //$NON-NLS-1$
            nameValuePairs.add(new BasicNameValuePair("TIMESTAMP", String.valueOf(0))); //$NON-NLS-1$
        }
        //multivalue parametr
        for (String dude : people){
            nameValuePairs.add(new BasicNameValuePair("USERS",dude)); //$NON-NLS-1$
        }
        return nameValuePairs;
    }

    /***
     * excutes http request in the connection manager
     * @param hr the request
     * @param arcm conneciton manager
     * @return input stream of the response
     * @throws ClientProtocolException
     * @throws IOException
     */
    private static InputStream requestToStream(HttpUriRequest hr, ConnectionManager arcm) throws ClientProtocolException, IOException{
        HttpResponse result = arcm.executeOnHttp(hr);
        InputStream is = result.getEntity().getContent();
        return is;
    }

    /***
     * create name value pairs for invitation mail server request
     * @param mail email to invite
     * @return the name value pairs
     */
    private static List<NameValuePair> createMailPostData(String mail){
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
        nameValuePairs.add(new BasicNameValuePair("FRIEND",mail)); //$NON-NLS-1$
        return nameValuePairs;
    }

    /***
     * make an array representation each of the properties in the node list
     * @param nodeLst
     * @param props
     * @return a list of string arrays of the extracted properties
     */
    private static List<String[]> extractPropsArray(NodeList nodeLst,String[] props) {
        List<String[]> lfp = new ArrayList<String[]>();

        for (int s = 0; s < nodeLst.getLength(); s++) {

            Node fstNode = nodeLst.item(s);
            String[] arr = new String[props.length];

            if (fstNode.getNodeType() == Node.ELEMENT_NODE) {

                Element fstElmnt = (Element) fstNode;

                for (int i =0 ; i<props.length;i++){
                    NodeList fstNmElmntLst = fstElmnt.getElementsByTagName(props[i]);
                    Element fstNmElmnt = (Element) fstNmElmntLst.item(0);
                    NodeList fstNm = fstNmElmnt.getChildNodes();
                    arr[i] = (((Node) fstNm.item(0)).getNodeValue());
                }

                lfp.add(arr);
            }
        }

        return lfp;
    }

    /***
     * sending request to get gps status of multiple users
     * @param myFp the current gps properties of the local user. may be null
     * @param people an array of people requested
     * @param arcm connection manager to execute the request
     * @return list of the gps status of the requested people (sorted by mail address)
     * @throws ClientProtocolException
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public static List<FriendProps> requestPeople(FriendProps myFp,String[] people, ConnectionManager arcm) throws ClientProtocolException, IOException, ParserConfigurationException, SAXException{
        // sending current location and request for users
        HttpPost http_post = new HttpPost(AroundRoidAppConstants.gpsUrl);
        http_post.setEntity(new UrlEncodedFormEntity(createPostData(myFp,people)));
        InputStream is  = requestToStream(http_post,arcm);
        //data is recieved. starts parsing:
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(is);
        doc.getDocumentElement().normalize();
        NodeList nodeLst = doc.getElementsByTagName(FriendProps.root);
        List<String[]> propsArrList = extractPropsArray(nodeLst,FriendProps.props);
        List<FriendProps> fpl = FriendProps.fromArrList(propsArrList);
        //parsing complete!
        return fpl;

    }

    /***
     * send a request to send invitation to a specific person
     * @param people the person's mail address
     * @param arcm connection manager to execute the request
     * @return true if the mail will be sent, otherwise false
     * @throws ClientProtocolException
     * @throws IOException
     */
    public static boolean inviteMail(String people, ConnectionManager arcm) throws ClientProtocolException, IOException{
        HttpPost http_post = new HttpPost(AroundRoidAppConstants.inviterUrl);
        http_post.setEntity(new UrlEncodedFormEntity(createMailPostData(people)));
        InputStream is  = requestToStream(http_post,arcm);
        byte[] buf = new byte[20];
        is.read(buf, 0, 4);
        return buf[0]=='s';
    }

    /*
    private static String convertStreamToString(InputStream is)

    throws IOException {
        /*
         * To convert the InputStream to String we use the
         * Reader.read(char[] buffer) method. We iterate until the
         * Reader return -1 which means there's no more data to
         * read. We use the StringWriter class to produce the string.
         * /
        if (is != null) {
            Writer writer = new StringWriter();
            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(
                        new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            return writer.toString();
        } else {
            return "";
        }
    }
    */



}
