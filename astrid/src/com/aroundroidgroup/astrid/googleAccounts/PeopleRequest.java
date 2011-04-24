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

import com.todoroo.astrid.activity.myService;

public class PeopleRequest {

    private static List<NameValuePair> createPostData(Location userLocation){
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
        nameValuePairs.add(new BasicNameValuePair("GPSLAT", String.valueOf(userLocation.getLatitude())));
        nameValuePairs.add(new BasicNameValuePair("GPSLON", String.valueOf(userLocation.getLongitude())));
        nameValuePairs.add(new BasicNameValuePair("USERS", "NaamaKeshet@gmail.comXXXtomer.keshet@gmail.comXXXa@b.comXXXg@c.com"));
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
        http_post.setEntity(new UrlEncodedFormEntity(createPostData(userLocation)));
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

    public static class FriendProps{

        public final static String root = "Friend"; //$NON-NLS-1$

        public final static String[] props = new String[]{"Latitude","Longtitude","Mail"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        private String lat,lon;

        private String mail;

        public String getLat() {
            return lat;
        }

        public void setLat(String lat) {
            this.lat = lat;
        }

        public String getLon() {
            return lon;
        }

        public void setLon(String lon) {
            this.lon = lon;
        }

        public String getMail() {
            return mail;
        }

        public void setMail(String mail) {
            this.mail = mail;
        }

        public FriendProps() {
            // TODO Auto-generated constructor stub
        }

        @Override
        public String toString(){
            return getMail() + "::" + getLat() + "::" + getLon(); //$NON-NLS-1$ //$NON-NLS-2$
        }

        public void loadArr (String[] arr){
            if (arr.length!=props.length){
                return;
            }
            setLat(arr[0]);
            setLon(arr[1]);
            setMail(arr[2]);
        }

        public static List<FriendProps> fromArrList(List<String[]> arrLst){
            List<FriendProps> fpl = new ArrayList<FriendProps>(arrLst.size());
            for(String[] arr : arrLst){
                FriendProps fp = new FriendProps();
                fp.loadArr(arr);
                fpl.add(fp);
            }
            return fpl;
        }

    }

}
