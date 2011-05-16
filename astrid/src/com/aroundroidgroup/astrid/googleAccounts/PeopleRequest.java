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

import android.location.Location;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.myService;

public class PeopleRequest {

    private static List<NameValuePair> createPostData(Location userLocation,String peopleString){
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
        nameValuePairs.add(new BasicNameValuePair("GPSLAT", String.valueOf(userLocation.getLatitude())));
        nameValuePairs.add(new BasicNameValuePair("GPSLON", String.valueOf(userLocation.getLongitude())));
        //nameValuePairs.add(new BasicNameValuePair("GPSLAT", String.valueOf("32.0")));
        //nameValuePairs.add(new BasicNameValuePair("GPSLON", String.valueOf("34.0")));
        //TODO : go bacj to userLastLocation
        nameValuePairs.add(new BasicNameValuePair("USERS",peopleString));//("USERS", "NaamaKeshet@gmail.comXXXtomer.keshet@gmail.comXXXa@b.comXXXg@c.com"));
        nameValuePairs.add(new BasicNameValuePair("TIMESTAMP", String.valueOf(DateUtilities.now())));
        return nameValuePairs;
    }

    private static InputStream requestToStream(HttpUriRequest hr) throws ClientProtocolException, IOException{
        HttpResponse result = myService.getHttpClient().execute(hr);
        InputStream is = result.getEntity().getContent();
        return is;
    }

    public static List<FriendProps> requestPeople(Location userLocation,String people) throws ClientProtocolException, IOException, ParserConfigurationException, SAXException{
        // sending current location and request for users
        HttpPost http_post = new HttpPost(AroundRoidAppConstants.gpsUrl);
        http_post.setEntity(new UrlEncodedFormEntity(createPostData(userLocation,people)));
        InputStream is  = requestToStream(http_post);
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
