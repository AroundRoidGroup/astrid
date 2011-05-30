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

import com.todoroo.andlib.utility.DateUtilities;

public class PeopleRequest {

    private static List<NameValuePair> createPostData(FriendProps currentLocation,String peopleString){
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
        if (currentLocation!=null){
            nameValuePairs.add(new BasicNameValuePair("GPSLAT", String.valueOf(currentLocation.getDlat()))); //$NON-NLS-1$
            nameValuePairs.add(new BasicNameValuePair("GPSLON", String.valueOf(currentLocation.getDlon())));
            //nameValuePairs.add(new BasicNameValuePair("GPSLAT", String.valueOf("32.0")));
            //nameValuePairs.add(new BasicNameValuePair("GPSLON", String.valueOf("34.0")));
            //TODO : go bacj to userLastLocation
            nameValuePairs.add(new BasicNameValuePair("TIMESTAMP", String.valueOf(currentLocation.getTime())));
        }
        else{
            nameValuePairs.add(new BasicNameValuePair("GPSLAT", String.valueOf(0.0)));
            nameValuePairs.add(new BasicNameValuePair("GPSLON", String.valueOf(0.0)));
            //nameValuePairs.add(new BasicNameValuePair("GPSLAT", String.valueOf("32.0")));
            //nameValuePairs.add(new BasicNameValuePair("GPSLON", String.valueOf("34.0")));
            //TODO : go bacj to userLastLocation
            nameValuePairs.add(new BasicNameValuePair("TIMESTAMP", String.valueOf(DateUtilities.now())));
        }
        nameValuePairs.add(new BasicNameValuePair("USERS",peopleString));//("USERS", "NaamaKeshet@gmail.comXXXtomer.keshet@gmail.comXXXa@b.comXXXg@c.com"));
        return nameValuePairs;
    }

    private static InputStream requestToStream(HttpUriRequest hr, AroundRoidConnectionManager arcm) throws ClientProtocolException, IOException{
        HttpResponse result = arcm.executeOnHttp(hr);
        InputStream is = result.getEntity().getContent();
        return is;
    }

    public static List<FriendProps> requestPeople(FriendProps currentLocation,String people, AroundRoidConnectionManager arcm) throws ClientProtocolException, IOException, ParserConfigurationException, SAXException{
        // sending current location and request for users
        HttpPost http_post = new HttpPost(AroundRoidAppConstants.gpsUrl);
        http_post.setEntity(new UrlEncodedFormEntity(createPostData(currentLocation,people)));
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

    private static List<NameValuePair> createMailPostData(String mail){
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
        nameValuePairs.add(new BasicNameValuePair("FRIEND",mail));
        return nameValuePairs;
    }

    //TODO CHANGE
    public static boolean inviteMail(String people, AroundRoidConnectionManager arcm) throws ClientProtocolException, IOException{
        // sending current location and request for users
        HttpPost http_post = new HttpPost(AroundRoidAppConstants.inviterUrl);
        http_post.setEntity(new UrlEncodedFormEntity(createMailPostData(people)));
        InputStream is  = requestToStream(http_post,arcm);
        byte[] buf = new byte[20];
        is.read(buf, 0, 4);
        return buf[0]=='s';
    }

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



}
