package testebs;

import java.io.IOException;
import java.util.*;

import org.jsoup.nodes.*;
import org.jsoup.select.*;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import io.github.cdimascio.dotenv.Dotenv.Filter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jsoup.Jsoup;
import org.jsoup.Connection.*;

class Lists {
  protected JsonArray citiAreaList, schulList;
  protected JsonObject jsonObject;
  private final String googleBot = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)";
  private final String googleSearch = "https://www.google.co.kr/search?q=";
  protected String schulCodeCookie, host, j_username, j_password;
  protected ArrayList<HashMap<String, String>> joindClasss, createdClasss, alctcrList, notCompleted;
  protected Map<String, String> cookies;
  String menuSn, atnlcNo, alctcrSn, stepSn, type, sessSn, meTy;

  protected void loginView() throws IOException {
    Document doc = Jsoup.connect("https://" + this.host + ".ebssw.kr/sso/loginView.do?loginType=onlineClass")
        .maxBodySize(0).ignoreContentType(true).userAgent(googleBot).get();
    String c = doc.getElementById("c").attr("value");
    String SAMLRequest = doc.getElementById("SAMLRequest").attr("value");
    String j_returnurl = doc.getElementById("j_returnurl").attr("value");
    String j_loginurl = doc.getElementById("j_loginurl").attr("value");
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

    this.cookies = res.cookies();
    this.cookies.put("LSCO", "");
    this.cookies.put("schulCodeCookie", this.schulCodeCookie);

    doc = Jsoup.connect("https://" + this.host + ".ebssw.kr/onlineClass/reqst/onlineClassReqstInfoView.do")
        .ignoreHttpErrors(true).ignoreContentType(true).maxBodySize(0).cookies(this.cookies).userAgent(googleBot)
        .timeout(0).get();

    this.joindClasss = new ArrayList<>();
    if (!doc.getElementsByClass("list").first().toString().contains("<ul"))
      return;
    for (Element element : doc.getElementsByClass("list").last().children())
      joindClasss.add(new HashMap<String, String>() {
        {
          put("URL", element.child(0).attr("href"));
          put("name", element.child(0).childNode(0).toString().trim());
        }
      });
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
  }

  public ArrayList<HashMap<String, String>> getJoindClasss() {
    return this.joindClasss;
  }

  protected void hmpgAlctcrListView() throws IOException {
    Elements alctcrElements;
    alctcrList = new ArrayList<HashMap<String, String>>();
    for (int i = 0; i < this.joindClasss.size(); i++) {
      alctcrElements = Jsoup.connect(this.joindClasss.get(i).get("URL")).userAgent(googleBot).maxBodySize(0)
          .cookies(this.cookies).get().getElementsByClass("list al list_type");
      this.menuSn = (menuSn = alctcrElements.first().child(0).child(0).attr("href")).contains("?")
          ? menuSn.substring(menuSn.indexOf("?"), menuSn.indexOf("&")).split("=")[1]
          : null;
      for (Element element : alctcrElements.first().children()) {
        alctcrList.add(new HashMap<String, String>() {
          {
            put("menuSn", menuSn);
            put("href", element.child(0).attr("href"));
            put("name", element.child(0).child(0).child(0).attr("alt"));
          }
        });
      }
    }
  }

  protected void hmpgAlctcrDetailView() throws IOException {
    hmpgAlctcrDetailView(0);
  }

  protected void hmpgAlctcrDetailView(int joindClassIndex) throws IOException {
    notCompleted = new ArrayList<>();
    HashMap<String, String> jcIndex = joindClasss
        .get(joindClassIndex = (joindClassIndex > -1 && joindClassIndex < joindClasss.size()) ? joindClassIndex : 0);
    for (HashMap<String, String> alctcr : alctcrList) {

      HashMap<String, String> postData = new HashMap<String, String>() {
        {
          Document doc;
          put("menuSn", menuSn);
          put("stepSn",
              stepSn = (doc = Jsoup.connect("https://" + host + ".ebssw.kr" + alctcr.get("href")).cookies(cookies)
                  .maxBodySize(0).ignoreContentType(true).userAgent(googleBot).timeout(0).get())
                      .getElementById("stepSn").attr("value"));
          put("atnlcNo", atnlcNo = doc.getElementById("atnlcNo").attr("value"));
          put("alctcrSn", alctcrSn = doc.getElementById("alctcrSn").attr("value"));
          put("tabType", type = doc.select(".menu__item > a").attr("id").replace("tabType_", ""));
          put("meTy", meTy = doc.getElementById("meTy") != null ? doc.getElementById("meTy").attr("value") : "");
        }
      };
      Document doc = Jsoup.connect(jcIndex.get("URL") + "/hmpg/hmpgLctrumTabView.do").data(postData)
          .followRedirects(true).cookies(cookies).userAgent(googleBot).ignoreContentType(true).maxBodySize(0).timeout(0)
          .header("Upgrade-Insecure-Requests", "1").post();
      for (Element element : doc.getElementsByClass("clearfix")) {
        if (!(element.childrenSize() > 1) || element.child(1).attr("class").split(" ")[1].contains("complete"))
          continue;
        notCompleted.add(new HashMap<String, String>() {
          {
            put("new_class_status", element.child(1).attr("class").split(" ")[1]);
            if (get("new_class_status").equals("ing")) {
              put("per", element.child(1).select(".per").text());
            }
            put("class_tit fl", element.child(0).select(".class_name").text());
            put("class_tit", alctcr.get("name"));
            put("logo txt_grey", jcIndex.get("name"));
          }
        });
      }
    }

  }

  protected void notifyNotCompleted() {
    for (HashMap nC : notCompleted) {
      System.out.println(nC);
    }
  }

  public static void main(String[] args) throws IOException {
    HashMap<String, String> dotenv = new HashMap<>();
    for (DotenvEntry e : Dotenv.configure().load().entries(Filter.DECLARED_IN_ENV_FILE)) {
      dotenv.put(e.getKey(), e.getValue());
    }
    Lists test = new Lists(dotenv.get("schoolName")) {
      {
        j_username = dotenv.get("username");
        j_password = dotenv.get("password");
      }
    };
    test.loginView();
    test.hmpgAlctcrListView();
    test.hmpgAlctcrDetailView();
    test.notifyNotCompleted();
    return;
  }
}