package testebs;

import java.io.IOException;
import java.util.*;

import org.jsoup.nodes.*;
import org.jsoup.select.*;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jsoup.Jsoup;
import org.jsoup.Connection.*;

class Lists {
  protected JsonArray citiAreaList, schulList;
  protected JsonObject jsonObject;
  private final String googleBot = "Mozilla/5.0 (compatible; Googlebot/2.1; http://www.google.com/bot.html)";
  private final String googleSearch = "https://www.google.co.kr/search?q=";
  private String schulCodeCookie, host, schulLevelCode, schulLevelParam;
  private ArrayList<HashMap<String, String>> joindClasss, createdClasss;

  private HashMap<String, String> loginView(String j_username, String j_password) throws IOException {
    Document doc = Jsoup.connect("https://" + this.host + ".ebssw.kr/sso/loginView.do?loginType=onlineClass")
        .maxBodySize(0).ignoreContentType(true).userAgent(googleBot).get();
    String c = doc.getElementById("c").attr("value");
    String SAMLRequest = doc.getElementById("SAMLRequest").attr("value");
    String j_returnurl = doc.getElementById("j_returnurl").attr("value");
    String j_loginurl = doc.getElementById("j_loginurl").attr("value");
    // String j_logintype = doc.getElementById("j_logintype").attr("value");
    String localLoginUrl = doc.getElementById("localLoginUrl").attr("value");
    String hmpgId = doc.getElementById("hmpgId").attr("value");
    String userSeCode = doc.getElementById("userSeCode").attr("value");
    String loginType = doc.getElementById("loginType").attr("value");
    HashMap<String, String> postData = new HashMap<String, String>() {
      {
        put("c", c);
        put("SAMLRequest", SAMLRequest);
        put("j_returnurl", j_returnurl);
        put("j_loginurl", j_loginurl);
        put("j_loginurl", j_loginurl);
        put("localLoginUrl", localLoginUrl);
        put("hmpgId", hmpgId);
        put("userSeCode", userSeCode);
        put("loginType", loginType);
        put("j_username", j_username);
        put("j_password", j_password);
      }
    };
    Response res = Jsoup.connect("https://" + this.host + ".ebssw.kr/sso").data(postData).method(Method.POST).execute();

    Map<String, String> loginCookies = res.cookies();
    /**
     * schulCodeCookie (Required) Same as schulCcode value
     */
    loginCookies.put("schulCodeCookie", this.schulCodeCookie);
    doc = Jsoup.connect("https://" + this.host + ".ebssw.kr/onlineClass/reqst/onlineClassReqstInfoView.do")
        .maxBodySize(0).cookies(loginCookies).userAgent(googleBot).get();

    // createdClasss = doc.getElementsByClass("list").first();
    this.joindClasss = new ArrayList<>();
    for (Element element : doc.getElementsByClass("list").last().children())
      joindClasss.add(new HashMap<String, String>() {
        {
          put("URL", element.child(0).attr("href"));
          put("OnlineClassName", element.child(0).childNode(0).toString().trim());
        }
      });

    return null;
  }

  private String googleSchul(String schNm) throws IOException {
    Document doc = Jsoup.connect(googleSearch + schNm).userAgent(googleBot).maxBodySize(0).get();
    Elements eeee;
    String schoolN8m = null,
        descrpt = (descrpt = (eeee = doc.select("span:contains(의)")).first().child(0).childNode(1).toString())
            .contains("대한민국")
                ? descrpt.contains("학교")
                    ? (schoolN8m = eeee.first().parentNode().childNode(1).childNode(0).childNode(0).childNode(0)
                        .toString().trim())
                    : (schoolN8m = null)
                : descrpt.split("의 ")[1].contains("학교")
                    ? Jsoup.connect(googleSearch + descrpt.split("의 ")[0]).userAgent(googleBot).get()
                        .select("span:contains(의)").first().text().split("의 ")[0].contains("대한민국")
                            ? (schoolN8m = eeee.first().parentNode().childNode(1).childNode(0).childNode(0).childNode(0)
                                .toString().trim())
                            : (schoolN8m = null)
                    : null;
    return schoolN8m;
  }

  Lists(String schoolN8m) throws IOException {
    Document doc = Jsoup.connect("https://oc.ebssw.kr/resource/schoolList.js").ignoreContentType(true).maxBodySize(0)
        .get();
    String jsonString = doc.body().childNode(0).toString().replace("var ", "").replace("schulJSONObj =", "")
        .replace(";", "").trim();
    this.jsonObject = new Gson().fromJson(jsonString, JsonObject.class);
    String schoolNmn = googleSchul(schoolN8m);
    this.citiAreaList = jsonObject.getAsJsonArray("citiAreaList");
    this.schulList = jsonObject.getAsJsonArray("schulList");

    for (int i = 0; i < schulList.size(); i++) {
      if (!schulList.get(i).toString().contains(schoolNmn))
        continue;
      this.jsonObject = schulList.get(i).getAsJsonObject();
    }
    this.schulCodeCookie = jsonObject.get("schulCcode").toString().replaceAll("\"", "");
    this.host = jsonObject.get("host").toString().replaceAll("\"", "");
    return;
  }
}
