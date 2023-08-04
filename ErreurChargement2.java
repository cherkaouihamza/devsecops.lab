package controle;
import java.io.*;
import java.sql.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.jdom.*;
import org.jdom.input.*;
public class ErreurChargement extends HttpServlet {
  private static final String CONTENT_TYPE = "text/html; charset=windows-1252";
  private static Document document;
  private static Element racine;
  private Connection con;
  private ResultSet rset;
  private Statement stmt;
  private String sql;
  private String sql2;
//test for github
  public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    response.setContentType(CONTENT_TYPE);
    PrintWriter out = response.getWriter();
    SAXBuilder sxb = new SAXBuilder();
    //out.println("je suis dans doPost de Erreur Chargement");
    String fichier = request.getParameter("file");
    //out.println("Fichier erreur chargement"+fichier);
    String fich = fichier.substring(0);
    try {
      document = sxb.build(new File(fich));
    } catch (JDOMException e) {
      afficheErreur(out, e);
    }
    out.println("<html><head><title>Contrôle des Entreprises d'Assurances - " +
      "Résultat du chargement des données</title><link href='css/blaf.css' " +
      "rel='stylesheet' media='screen'/></head>");
    out.println("<body><h1>Résultat du chargement des données</h1>");
    boolean errorWritten = true;
    int isValide = fileNameWithXMLData(fichier);
    if (isValide == 0) {
      try {
        conn(request.getParameter("nom"), request.getParameter("pass"),
          request.getParameter("db"));
        isValide = XMLDataWithDB();
      } catch (SQLException e) {out.println("erreur 24 connexion");
        isValide = 24;
      }
    } else if (errorWritten) {
      out.println("<p><b><font color='#ff0000' size='4'>ERREUR : " +
        errorMsg(isValide) + "</font></b></p>");
      errorWritten = false;
    }
    if (isValide == 0) {
      try {
        isValide = XMLDataWithDBNumbers(request.getParameter("maxLig"),
            request.getParameter("maxCol"), request.getParameter("nbrCol"),
            request.getParameter("maxChamp"), request.getParameter("nbrChamp"));
      } catch (SQLException e) {out.println("erreur 24 maxlig nbr col...");
        isValide = 24;
      }
    } else if (errorWritten) {
      out.println("<p><b><font color='#ff0000' size='4'>ERREUR : " +
        errorMsg(isValide) + "</font></b></p>");
      errorWritten = false;
    }
    if (isValide == 0) {
      racine = document.getRootElement();
      String codeEntreprise = racine.getChild("Identification")
        .getChild("CodeEntreprise").getText();
      String codeExe = racine.getChild("Identification").getChild("Exercice")
        .getText();
      String codePer = racine.getChild("Identification").getChild("Periode")
        .getText();
      int exercice = Integer.parseInt(codeExe);
      int periode = Integer.parseInt(codePer);
      String codeEtat = racine.getChild("Identification").getChild("CodeEtat")
        .getText();
      String codeTableau = racine.getChild("Identification")
        .getChild("CodeTableau").getText();
      String codeCombinaison = racine.getChild("Identification")
        .getChild("CodeCombinaison").getText();
      String v = "";
      try {
        v = version("T", codeEntreprise, codeEtat, codeTableau, codeExe, codePer);
        System.out.println("j'ai cherche la version qui est egale à "+v);
        out.println("<h2>Identification du tableau</h2><p/>");
        out.println("<table cellspacing='2' cellpadding='3' " +
          "border='2' width='100%'>");
        sql = "select nom_lg_entre from controle.info_entre where cde_entre = '" +
          codeEntreprise + "'";
        System.out.println("la requete est "+sql);  
        rset = stmt.executeQuery(sql);
        String nomEntreprise = "";
        if (rset.next()) {
          nomEntreprise = rset.getString(1);
          System.out.println("le nom entreprise est "+nomEntreprise);
        }
        rset.close();
        out.println("<tr class='r0'><td width='25%'>Entreprise : " +
          "</td><td width='25%'>" + codeEntreprise + "</td><td width='50%'>" +
          nomEntreprise + "</td></tr>");
        out.println("<tr class='r1'><td width='25%'>Exercice : " +
          "</td><td width='25%'>" + exercice + "</td><td width='50%'>" +
          exercice + "</td></tr>");
        out.println("<tr class='r0'><td width='25%'>Période : " +
          "</td><td width='25%'>" + periode + "</td><td width='50%'>" +
          periode + "</td></tr>");
        sql = "select dsg_eta from det_etat where cde_eta = '" + codeEtat +
          "' and ver_eta = " +
          version("E", codeEntreprise, codeEtat, "", codeExe, codePer);
        rset = stmt.executeQuery(sql);
        String desEtat = "";
        if (rset.next()) {
          desEtat = rset.getString(1);
        }
        rset.close();
        out.println("<tr class='r1'><td width='25%'>Etat : " +
          "</td><td width='25%'>" + codeEtat + "</td><td width='50%'>" +
          desEtat + "</td></tr>");
        sql = "select nom_lgn, nvl(nom_sig,'') from tableau where cde_eta = '" +
          codeEtat + "' and cde_tab = '" + codeTableau + "' and cde_ver = " +
          v;
        out.println("valeurs à ce niveau :"+sql);  
        rset = stmt.executeQuery(sql);
        String desTableau = "";
        if (rset.next()) {
          desTableau = rset.getString(1) + " - " + rset.getString(2);
        }
        rset.close();
        if ((desTableau.indexOf(" - ") + 3) == desTableau.length()) {
         desTableau = desTableau.substring(0, desTableau.length() - 3);
        } else if ((desTableau.indexOf(" - null") + 7) == desTableau.length()) {
         desTableau = desTableau.substring(0, desTableau.length() - 7);
        }
        out.println("<tr class='r0'><td width='25%'>Tableau : " +
          "</td><td width='25%'>" + codeTableau + "</td><td width='50%'>" +
          desTableau + "</td></tr>");
        sql = "select r.dsg_rub from rubrique r, det_combinaison d where" +
          " r.cde_rub = d.cde_rub and d.cde_eta = '" + codeEtat +
          "' and d.cde_tab = '" + codeTableau + "' and d.cde_ver = " + v +
          " and d.cde_com = '" + codeCombinaison +  "'";
        System.out.println("la fameuse requete = "+sql);
        rset = stmt.executeQuery(sql);
        String desCombinaison = "";
        while (rset.next()) {
          desCombinaison += (" - " + rset.getString(1));
        }
        rset.close();
        System.out.println("desCombinaison = "+desCombinaison);
        desCombinaison = desCombinaison.substring(3);
        out.println("<tr class='r1'><td width='25%'>Combinaison : " +
          "</td><td width='25%'>" + codeCombinaison + "</td><td width='50%'>" +
          desCombinaison + "</td></tr>");
        out.println("</table>");
      } catch (SQLException e) {
        isValide = 24;
      }
      boolean continuer = true;
      int nbre = 0;
      if (!(request.getParameter("nbrChamp").equals("0"))) {
        List listChampSup = racine.getChild("DonneesSup").getChildren("Champ");
        Iterator ic = listChampSup.iterator();
        int ordreChamp = 0;
        String valeurChamp = "";
        String codeChamp = "";
        String typeDonChamp = "";
        while (ic.hasNext() && continuer) {
          Element champCourant = (Element) ic.next();
          ordreChamp = Integer.parseInt(champCourant.getAttribute("ordre")
              .getValue());
          valeurChamp = champCourant.getText();
          valeurChamp = valeurChamp.replaceAll("'", "''");
          try {
            sql = "select cde_rub, typ_don_chp from chp_supplement where cde_eta = '" +
              codeEtat + "' and cde_tab = '" + codeTableau +
              "' and cde_ver = '" + v + "' and ord_chp = " + ordreChamp;
            rset = stmt.executeQuery(sql);
            if (rset.next()) {
              codeChamp = rset.getString(1);
              typeDonChamp = rset.getString(2);
            }
            rset.close();
            sql = "insert into don_chp_supplement values ('" + codeEtat +
              "', '" + codeTableau + "', '" + v + "', '" + codeEntreprise +
              "', " + exercice + ", " + periode + ", '" + codeChamp + "', " +
              ordreChamp + ", ";
            if (typeDonChamp.equals("P") || typeDonChamp.equals("N") ||
              typeDonChamp.equals("E")) {
              sql += ("'" + valeurChamp + "', '', '')");
            } else if (typeDonChamp.equals("V")) {
              sql += ("'', '" + valeurChamp + "','')");
            } else if (typeDonChamp.equals("D")) {
              sql += ("'', '','" + valeurChamp + "')");
            }
            nbre = stmt.executeUpdate(sql);
            if (nbre == 0) {
              sql = "delete from don_chp_supplement where cde_eta = '" +
                codeEtat + "' and cde_tab = '" + codeTableau +
                "' and cde_ent = '" + codeEntreprise + "' and cde_exe = " +
                exercice + " and cde_moi = " + periode;
              nbre = stmt.executeUpdate(sql);
              out.println("<p><b><font color='#ff0000' size='4'> " +
                "Erreur d'insertion : Etat = " + codeEtat + ", Tableau = " +
                codeTableau + ", Entreprise = " + codeEntreprise +
                ", Exercice = " + exercice + ", Période = " + periode +
                ", Ordre champ supplémentaire = " + ordreChamp +
                " et Valeur champ supplémentaire = " + valeurChamp + "<br/>");
              out.println("Les autres lignes déjà insérées dans ce tableau " +
                "seront supprimées.</p></b></font>");
              continuer = false;
              errorWritten = false;
            }
          } catch (SQLException e) {
            isValide = 24;
            continuer = false;
          }
        }
      }
      String typEcr = "";
      int ordreLigne = 0;
      int ordreColonne = 0;
      String valeur = "";
      if (continuer) {
        List listLignes = racine.getChild("Donnees").getChildren("Ligne");
        Iterator i = listLignes.iterator();
        while (i.hasNext() && continuer) {
          Element ligneCourante = (Element) i.next();
          ordreLigne = Integer.parseInt(ligneCourante.getAttribute("ordre")
              .getValue());
          List listColonnes = ligneCourante.getChildren("Colonne");
          Iterator j = listColonnes.iterator();
          while (j.hasNext() && continuer) {
            Element colonneCourante = (Element) j.next();
            ordreColonne = Integer.parseInt(colonneCourante.getAttribute(
                  "ordre").getValue());
            valeur = colonneCourante.getText();
            valeur = valeur.replaceAll("'", "''");
            try {
              sql = "select count(*) from cel_creuses where cde_eta = '" +
                codeEtat + "' and cde_tab = '" + codeTableau +
                "' and cde_ver = '" + v + "' and ord_lig = " + ordreLigne +
                " and ord_col = " + ordreColonne;
              rset = stmt.executeQuery(sql);
              if (rset.next()) {
                if (rset.getInt(1) == 1) {
                  isValide = 23;
                  out.println("<p><b><font color='#ff0000' size='4'>ERREUR : " +
                    errorMsg(isValide) + "</font></b></p>");
                  errorWritten = false;
                  continuer = false;
                }
              }
              rset.close();
              sql = "select count(*) from tab_ligne where cde_eta = '" +
                codeEtat + "' and cde_tab = '" + codeTableau +
                "' and cde_ver = '" + v + "' and ord_lig = " + ordreLigne +
                " and typ_lig = 'T'";
              rset = stmt.executeQuery(sql);
              if (rset.next()) {
                if (rset.getInt(1) == 1) {
                  isValide = 23;
                  out.println("<p><b><font color='#ff0000' size='4'>ERREUR : " +
                    errorMsg(isValide) + "</font></b></p>");
                  errorWritten = false;
                  continuer = false;
                }
              }
              rset.close();
              if (isValide == 0) {
                sql = "select typ_ecr from tableau where cde_eta = '" +
                  codeEtat + "' and cde_tab = '" + codeTableau +
                  "' and cde_ver = '" + v + "'";
                rset = stmt.executeQuery(sql);
                if (rset.next()) {
                  typEcr = rset.getString(1);
                }
                rset.close();
                String codeLigne = (new Integer(ordreLigne)).toString();
                if (typEcr.equals("ECRAN1")) {
                  sql = "select cde_lig from tab_ligne where cde_eta = '" +
                    codeEtat + "' and cde_tab = '" + codeTableau +
                    "' and cde_ver = '" + v + "' and ord_lig = " + ordreLigne;
                  rset = stmt.executeQuery(sql);
                  if (rset.next()) {
                    codeLigne = rset.getString(1);
                  }
                  rset.close();
                }
                sql = "select cde_col, typ_don from tab_colonne where cde_eta = '" +
                  codeEtat + "' and cde_tab = '" + codeTableau +
                  "' and cde_ver = '" + v + "' and ord_col = " + ordreColonne;
                rset = stmt.executeQuery(sql);
                String codeColonne = "";
                String typeDonnees = "";
                if (rset.next()) {
                  codeColonne = rset.getString(1);
                  typeDonnees = rset.getString(2);
                }
                rset.close();
                sql = "insert into DONNEES";
                if (typEcr.equals("ECRAN2")) {
                  sql += "_BIS";
                }
                sql += (" values ('" + codeEtat + "', '" + codeTableau +
                "', '" + v + "', '" + codeEntreprise + "', " + exercice + ", " +
                periode + ", '" + codeCombinaison + "', '" + codeLigne + "', ");
                if (typEcr.equals("ECRAN2")) {
                  sql += (ordreLigne + ", '" + codeColonne + "', " +
                  ordreColonne + ", ");
                } else {
                  sql += ("'" + codeColonne + "', " + ordreLigne + ", " +
                  ordreColonne + ", ");
                }
                if (typeDonnees.equals("P") || typeDonnees.equals("N") ||
                  typeDonnees.equals("E")) {
                  sql += ("'" + valeur + "', '', '')");
                } else if (typeDonnees.equals("V")) {
                  sql += ("'', '" + valeur + "','')");
                } else if (typeDonnees.equals("D")) {
                  sql += ("'', '','" + valeur + "')");
                }
                nbre = stmt.executeUpdate(sql);
                if (nbre == 0) {
                  sql = "delete from DONNEES";
                  if (typEcr.equals("ECRAN2")) {
                    sql += "_BIS";
                  }
                  sql += (" where cde_eta = '" + codeEtat +
                  "' and cde_tab = '" + codeTableau + "' and cde_com = '" +
                  codeCombinaison + "' and cde_ent = '" + codeEntreprise +
                  "' and cde_exe = " + exercice + " and cde_moi = " + periode);
                  nbre = stmt.executeUpdate(sql);
                  if (!(request.getParameter("nbrChamp").equals("0"))) {
                    sql = "delete from don_chp_supplement where cde_eta = '" +
                      codeEtat + "' and cde_tab = '" + codeTableau +
                      "' and cde_ent = '" + codeEntreprise +
                      "' and cde_exe = " + exercice + " and cde_moi = " +
                      periode;
                    nbre = stmt.executeUpdate(sql);
                  }
                  out.println("<p><b><font color='#ff0000' size='4'> " +
                    "Erreur d'insertion : Etat = " + codeEtat + ", Tableau = " +
                    codeTableau + ", Combinaison = " + codeCombinaison +
                    ", Entreprise = " + codeEntreprise + ", Exercice = " +
                    exercice + ", Période = " + periode + ", Ordre ligne = " +
                    ordreLigne + ", Ordre colonne = " + ordreColonne +
                    " et Valeur = " + valeur + "<br/>");
                  out.println(
                    "Les autres lignes déjà insérées dans ce tableau " +
                    "seront supprimées.</p></b></font>");
                  continuer = false;
                  errorWritten = false;
                }
              }
            } catch (SQLException e) {
              isValide = 24;
              continuer = false;
            }
          }
        }
      }
      try {
        if (continuer) {
          out.println("<p/><font color ='#0000ff' size='3'>Les données de ce " +
            "tableau sont insérées avec succès dans la base de données.</font>");
          sql = "select cde_rub, ord_chp from chp_supplement where cde_eta = '" +
            codeEtat + "' and cde_tab = '" + codeTableau + "' and cde_ver = " +
            v + " and ord_chp not in (select ord_chp from don_chp_supplement" +
            " where cde_eta = '" + codeEtat + "' and cde_tab = '" +
            codeTableau + "' and cde_ver = " + v + " and cde_ent = '" +
            codeEntreprise + "' and cde_exe = " + exercice + " and cde_moi = " +
            periode + ")";
          rset = stmt.executeQuery(sql);
          Statement stmt1 = con.createStatement();
          while (rset.next()) {
            String sql1 = "insert into don_chp_supplement values ('" +
              codeEtat + "', '" + codeTableau + "', " + v + ", '" +
              codeEntreprise + "', " + exercice + ", " + periode + ", '" +
              rset.getString(1) + "', " + rset.getString(2) + ", '', '', '')";
            int nbr = stmt1.executeUpdate(sql1);
          }
          rset.close();
          stmt1.close();
          if (typEcr.equals("ECRAN1")) {
            sql = "select l.ord_lig, l.cde_lig, c.ord_col, c.cde_col from " +
              "tab_ligne l, tab_colonne c where l.cde_eta = c.cde_eta and " +
              "l.cde_tab = c.cde_tab and l.cde_ver = c.cde_ver and l.cde_eta = '" +
              codeEtat + "' and l.cde_tab = '" + codeTableau +
              "' and l.cde_ver = '" + v + "' and l.typ_lig != 'T'" +
              " and (l.ord_lig, c.ord_col) not in (select ord_lig," +
              " ord_col from cel_creuses where cde_eta = l.cde_eta and " +
              "cde_tab = l.cde_tab and cde_ver = l.cde_ver) and (l.ord_lig, " +
              "c.ord_col) not in (select ord_lig, ord_col from donnees" +
              " where cde_eta = '" + codeEtat + "' and cde_tab = '" +
              codeTableau + "' and cde_ver = '" + v + "' and cde_ent = '" +
              codeEntreprise + "' and cde_exe = '" + exercice +
              "' and cde_moi = '" + periode + "' and cde_com = '" +
              codeCombinaison + "')";
            rset = stmt.executeQuery(sql);
            Statement stmt2 = con.createStatement();
            while (rset.next()) {
              String sql1 = "insert into donnees values ('" + codeEtat +
                "', '" + codeTableau + "', '" + v + "', '" + codeEntreprise +
                "', " + exercice + ", " + periode + ", '" + codeCombinaison +
                "', '" + rset.getString(2) + "', '" + rset.getString(4) +
                "', " + rset.getString(1) + ", " + rset.getString(3) +
                ", '', '', '')";
              int nbr = stmt2.executeUpdate(sql1);
            }
            rset.close();
            stmt2.close();
          } else if (typEcr.equals("ECRAN2")) {
            sql = "select max(ord_lig) from donnees_bis where cde_eta = '" +
              codeEtat + "' and cde_tab = '" + codeTableau +
              "' and cde_ver = '" + v + "' and cde_ent = '" + codeEntreprise +
              "' and cde_exe = '" + exercice + "' and cde_moi = '" + periode +
              "' and cde_com = '" + codeCombinaison + "'";
            rset = stmt.executeQuery(sql);
            int maxLig = 0;
            if (rset.next()) {
              maxLig = rset.getInt(1);
            }
            rset.close();
            for (int ligCour = 1; ligCour <= maxLig; ligCour++) {
              sql = "select cde_col, ord_col from tab_colonne where " +
                "cde_eta = '" + codeEtat + "' and cde_tab = '" + codeTableau +
                "' and cde_ver = '" + v +
                "' and ord_col not in (select ord_col from donnees_bis" +
                " where cde_eta = '" + codeEtat + "' and cde_tab = '" +
                codeTableau + "' and cde_ver = '" + v + "' and cde_ent = '" +
                codeEntreprise + "' and cde_exe = '" + exercice +
                "' and cde_moi = '" + periode + "' and cde_com = '" +
                codeCombinaison + "' and ord_lig = " + ligCour + ")";
              rset = stmt.executeQuery(sql);
              Statement stmt3 = con.createStatement();
              while (rset.next()) {
                String sql1 = "insert into donnees_bis values ('" + codeEtat +
                  "', '" + codeTableau + "', '" + v + "', '" + codeEntreprise +
                  "', " + exercice + ", " + periode + ", '" + codeCombinaison +
                  "', '" + ligCour + "', " + ligCour + ", '" +
                  rset.getString(1) + "', " + rset.getString(2) +
                  ", '', '', '')";
                int nbr = stmt3.executeUpdate(sql1);
              }
              rset.close();
              stmt3.close();
            }
          }
        } else {
          sql = "delete from DONNEES";
          if (typEcr.equals("ECRAN2")) {
            sql += "_BIS";
          }
          sql += (" where cde_eta = '" + codeEtat + "' and cde_tab = '" +
          codeTableau + "' and cde_com = '" + codeCombinaison +
          "' and cde_ent = '" + codeEntreprise + "' and cde_exe = " + exercice +
          " and cde_moi = " + periode);
          nbre = stmt.executeUpdate(sql);
          if (!(request.getParameter("nbrChamp").equals("0"))) {
            sql = "delete from don_chp_supplement where cde_eta = '" +
              codeEtat + "' and cde_tab = '" + codeTableau +
              "' and cde_ent = '" + codeEntreprise + "' and cde_exe = " +
              exercice + " and cde_moi = " + periode;
            nbre = stmt.executeUpdate(sql);
          }
          out.println("<p><b><font color='#ff0000' size='4'> " +
            "Erreur d'insertion : Etat = " + codeEtat + ", Tableau = " +
            codeTableau + ", Combinaison = " + codeCombinaison +
            ", Entreprise = " + codeEntreprise + ", Exercice = " + exercice +
            ", Période = " + periode + ", Ordre ligne = " + ordreLigne +
            ", Ordre colonne = " + ordreColonne + " et Valeur = " + valeur +
            "<br/>");
          out.println("Les autres lignes déjà insérées dans ce tableau " +
            "seront supprimées.</p></b></font>");
        }
      } catch (SQLException e) {
        isValide = 24;
      }
    } else if (errorWritten) {
      out.println("<p><b><font color='#ff0000' size='4'>ERREUR : " +
        errorMsg(isValide) + "</font></b></p>");
      errorWritten = false;
    }
    out.println("<form method='post' action='verifierconnexion'>");
    out.println("<input type='hidden' name='nom' value='" +
      request.getParameter("nom") + "'/>");
    out.println("<input type='hidden' name='pass' value='" +
      request.getParameter("pass") + "'/>");
    out.println("<input type='hidden' name='db' value='" +
      request.getParameter("db") + "'/>");
    out.println("<input type='submit' value='un autre fichier'/>");
    out.println("</form></body></html>");
    out.close();
  }
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    System.out.println("apres init");
  }
  private int XMLDataWithDB() throws SQLException {
    String XMLcde_ent = document.getRootElement().getChild("Identification")
      .getChild("CodeEntreprise").getText();
    String XMLcde_exe = document.getRootElement().getChild("Identification")
      .getChild("Exercice").getText();
    String XMLcde_moi = document.getRootElement().getChild("Identification")
      .getChild("Periode").getText();
    String XMLcde_eta = document.getRootElement().getChild("Identification")
      .getChild("CodeEtat").getText();
    String XMLcde_tab = document.getRootElement().getChild("Identification")
      .getChild("CodeTableau").getText();
    String XMLcde_com = document.getRootElement().getChild("Identification")
      .getChild("CodeCombinaison").getText();
    sql = "select cde_entre from controle.info_entre where cde_entre = '" + XMLcde_ent +
      "'";
    //out.println("00000");
    rset = stmt.executeQuery(sql);
    if (!(rset.next())) {
      rset.close();
      return 9;
    }
    rset.close();
    sql = "select distinct cde_exe from exe_comptable where cde_exe = '" +
      XMLcde_exe + "'";
    rset = stmt.executeQuery(sql);
    if (!(rset.next())) {
      rset.close();
      return 10;
    }
    rset.close();
    sql = "select cde_moi from exe_comptable where cde_exe = '" + XMLcde_exe +
      "' and cde_moi = '" + XMLcde_moi + "'";
    rset = stmt.executeQuery(sql);
    if (!(rset.next())) {
      rset.close();
      return 11;
    }
    rset.close();
    sql = "select cde_eta from eta_entreprise where cde_eta = '" + XMLcde_eta +
      "' and ver_eta = '" +
      version("E", XMLcde_ent, XMLcde_eta, "", XMLcde_exe, XMLcde_moi) +
      "' and cde_ent = '" + XMLcde_ent + "'";
    rset = stmt.executeQuery(sql);
    if (!(rset.next())) {
      rset.close();
      return 12;
    }
    rset.close();
    sql = "select cde_tab from tableau where cde_eta = '" + XMLcde_eta +
      "' and cde_tab = '" + XMLcde_tab + "' and cde_ver = '" +
      version("T", XMLcde_ent, XMLcde_eta, XMLcde_tab, XMLcde_exe, XMLcde_moi) +
      "'";
    rset = stmt.executeQuery(sql);
    if (!(rset.next())) {
      rset.close();
      return 13;
    }
    rset.close();
    sql = "select cde_com from tab_combinaison where cde_eta = '" + XMLcde_eta +
      "' and cde_tab = '" + XMLcde_tab + "' and cde_ver = '" +
      version("T", XMLcde_ent, XMLcde_eta, XMLcde_tab, XMLcde_exe, XMLcde_moi) +
      "' and cde_com = '" + XMLcde_com + "'";
    rset = stmt.executeQuery(sql);
    if (!(rset.next())) {
      rset.close();
      return 14;
    }
    rset.close();
    sql = "select frq_rec from det_etat where cde_eta = '" + XMLcde_eta +
      "' and ver_eta = '" +
      version("E", XMLcde_ent, XMLcde_eta, "", XMLcde_exe, XMLcde_moi) + "'";
    rset = stmt.executeQuery(sql);
    int freqRec = 0;
    if (rset.next()) {
      freqRec = rset.getInt(1);
      if (Integer.parseInt(XMLcde_moi) > freqRec) {
        rset.close();
        return 15;
      }
    }
    rset.close();
    String typEcr = "";
    sql = "select typ_ecr from tableau where cde_eta = '" + XMLcde_eta +
      "' and cde_tab = '" + XMLcde_tab + "' and cde_ver = '" +
      version("T", XMLcde_ent, XMLcde_eta, XMLcde_tab, XMLcde_exe, XMLcde_moi) +
      "'";
    rset = stmt.executeQuery(sql);
    if (rset.next()) {
      typEcr = rset.getString(1);
    }
    rset.close();
    if (typEcr.equals("ECRAN1")) {
      sql = "select max(cde_exe) from donnees where cde_ent = '" + XMLcde_ent +
        "' and cde_eta = '" + XMLcde_eta + "' and cde_tab = '" + XMLcde_tab +
        "' and cde_com = '" + XMLcde_com + "'";
    } else {
      sql = "select max(cde_exe) from donnees_bis where cde_ent = '" +
        XMLcde_ent + "' and cde_eta = '" + XMLcde_eta + "' and cde_tab = '" +
        XMLcde_tab + "' and cde_com = '" + XMLcde_com + "'";
    }
    rset = stmt.executeQuery(sql);
    int maxExercice = 2005;
    boolean exeDefault = true;
    if (rset.next()) {
      maxExercice = rset.getInt(1);
      exeDefault = false;
    }
    rset.close();
    if (typEcr.equals("ECRAN1")) {
      sql = "select max(cde_moi) from donnees where cde_ent = '" + XMLcde_ent +
        "' and cde_exe = '" + maxExercice + "' and cde_eta = '" + XMLcde_eta +
        "' and cde_tab = '" + XMLcde_tab + "' and cde_com = '" + XMLcde_com +
        "'";
    } else {
      sql = "select max(cde_moi) from donnees_bis where cde_ent = '" +
        XMLcde_ent + "' and cde_exe = '" + maxExercice + "' and cde_eta = '" +
        XMLcde_eta + "' and cde_tab = '" + XMLcde_tab + "' and cde_com = '" +
        XMLcde_com + "'";
    }
    rset = stmt.executeQuery(sql);
    int maxPeriode = 1;
    boolean perDefault = true;
    if (rset.next()) {
      maxPeriode = rset.getInt(1);
      perDefault = false;
    }
    rset.close();
    sql = "select frq_rec from det_etat where cde_eta = '" + XMLcde_eta +
      "' and ver_eta = " +
      version("E", XMLcde_ent, XMLcde_eta, "",
        (new Integer(maxExercice)).toString(),
        (new Integer(maxPeriode)).toString());
    rset = stmt.executeQuery(sql);
    int freqRecPrec = 0;
    if (rset.next()) {
      freqRecPrec = rset.getInt(1);
    }
    rset.close();
    return 0;
  }
  private int XMLDataWithDBNumbers(String maxLig, String maxCol,
    String nbrColXML, String maxChamp, String nbrChampXML)
    throws SQLException {
    String XMLcde_ent = document.getRootElement().getChild("Identification")
      .getChild("CodeEntreprise").getText();
    String XMLcde_exe = document.getRootElement().getChild("Identification")
      .getChild("Exercice").getText();
    String XMLcde_moi = document.getRootElement().getChild("Identification")
      .getChild("Periode").getText();
    String XMLcde_eta = document.getRootElement().getChild("Identification")
      .getChild("CodeEtat").getText();
    String XMLcde_tab = document.getRootElement().getChild("Identification")
      .getChild("CodeTableau").getText();
    String XMLcde_com = document.getRootElement().getChild("Identification")
      .getChild("CodeCombinaison").getText();
    if (maxChamp.equals("")) {
      maxChamp = "0";
    }
    sql = "select count(*) from chp_supplement where cde_eta = '" + XMLcde_eta +
      "' and cde_tab = '" + XMLcde_tab + "' and cde_ver = '" +
      version("T", XMLcde_ent, XMLcde_eta, XMLcde_tab, XMLcde_exe, XMLcde_moi) +
      "'";
    rset = stmt.executeQuery(sql);
    int nbrChamp = 0;
    if (rset.next()) {
      nbrChamp = rset.getInt(1);
    }
    rset.close();
    sql = "select nbr_lig, nbr_col, typ_ecr from tableau where cde_eta = '" +
      XMLcde_eta + "' and cde_tab = '" + XMLcde_tab + "' and cde_ver = '" +
      version("T", XMLcde_ent, XMLcde_eta, XMLcde_tab, XMLcde_exe, XMLcde_moi) +
      "'";
    rset = stmt.executeQuery(sql);
    int nbrLig = 0;
    int nbrCol = 0;
    String typEcr = "";
    if (rset.next()) {
      nbrLig = rset.getInt(1);
      nbrCol = rset.getInt(2);
      typEcr = rset.getString(3);
    }
    rset.close();
    int nbrColDB = nbrLig * nbrCol;
    sql = "select count(*) from cel_creuses where cde_eta = '" + XMLcde_eta +
      "' and cde_tab = '" + XMLcde_tab + "' and cde_ver = '" +
      version("T", XMLcde_ent, XMLcde_eta, XMLcde_tab, XMLcde_exe, XMLcde_moi) +
      "'";
    rset = stmt.executeQuery(sql);
    if (rset.next()) {
      nbrColDB -= rset.getInt(1);
    }
    rset.close();
    sql = "select count(*) from tab_ligne where cde_eta = '" + XMLcde_eta +
      "' and cde_tab = '" + XMLcde_tab + "' and cde_ver = '" +
      version("T", XMLcde_ent, XMLcde_eta, XMLcde_tab, XMLcde_exe, XMLcde_moi) +
      "' and typ_lig = 'T'";
    System.out.println("la requete pour chercher le nombre de lignes est : "+sql);  
    rset = stmt.executeQuery(sql);
    if (rset.next()) {
      nbrColDB -= (rset.getInt(1) * nbrCol);
      System.out.println("nombre de tab_ligne "+rset.getInt(1));
      System.out.println("nombre de colonnes "+nbrCol);
      System.out.println("nombre de cellules calculé "+nbrColDB);
    }
    rset.close();
    sql = "select count(*) from donnees";
    if (typEcr.equals("ECRAN2")) {
      sql += "_bis ";
    }
    sql += " where cde_ent='" + XMLcde_ent + "' and cde_exe=" + XMLcde_exe +
    " and cde_moi=" + XMLcde_moi + " and cde_eta='" + XMLcde_eta +
    "' and cde_tab='" + XMLcde_tab + "' and cde_com='" + XMLcde_com + "'";
    rset = stmt.executeQuery(sql);
    System.out.println("une autre requete :"+sql);
    int nbrData = 0;
    if (rset.next()) {
      nbrData = rset.getInt(1);
    }
    rset.close();
    sql = "select count(*) from don_chp_supplement where cde_ent='" +
      XMLcde_ent + "' and cde_exe=" + XMLcde_exe + " and cde_moi=" +
      XMLcde_moi + " and cde_eta='" + XMLcde_eta + "' and cde_tab='" +
      XMLcde_tab + "'";
    rset = stmt.executeQuery(sql);
    System.out.println("requete suivante 2 est "+sql);
    if (rset.next()) {
      nbrData += rset.getInt(1);
    }
    rset.close();
    if (nbrData > 0) {
      return 25;
    }
    if (Integer.parseInt(maxChamp) > nbrChamp) {
      return 26;
    }
    if (Integer.parseInt(nbrChampXML) > nbrChamp) {
      return 27;
    }
    if ((Integer.parseInt(maxLig) > nbrLig) && typEcr.equals("ECRAN1")) {
      return 20;
    }
    if (Integer.parseInt(maxCol) > nbrCol) {
      return 21;
    }
    if ((Integer.parseInt(nbrColXML) > nbrColDB) && typEcr.equals("ECRAN1")) {
      System.out.println("nombre cellules fichier XML "+Integer.parseInt(nbrColXML));
      System.out.println("nombre de cellules dans la base "+nbrColDB);
      return 22;
    }
    return 0;
  }
  private void afficheErreur(PrintWriter out, Exception e) {
    out.println("<html><head><title>Contrôle des Entreprises d'Assurances - " +
      "ERREUR</title><link href='css/blaf.css' rel='stylesheet' media='screen'/>");
    out.println("</head><body><h1>ERREUR</h1>");
    out.println("<p><b><font color='#ff0000' size='4'>" + e.getMessage() +
      "</font></b></p></body></html>");
  }
  private void conn(String name, String pass, String db)
    throws SQLException {
    DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
    con = DriverManager.getConnection("jdbc:oracle:thin:@" + db, name, pass);
    stmt = con.createStatement();
  }
  private String errorMsg(int errorCode) {
    switch (errorCode) {
      case 1 :
        return "Code de l'erreur = 1 : L'extension du fichier à charger" +
        " n'est pas .xml.";
      case 2 :
        return "Code de l'erreur = 2 : Le format du nom du fichier à charger" +
        " n'est pas correct : " +
        "CodeEntreprise-Exercice-Periode-CodeEtat-CodeTableau-CodeCombinaison.xml.";
      case 3 :
        return "Code de l'erreur = 3 : Il y a une discordance dans le code" +
        " entreprise entre le nom du fichier et la balise 'CodeEntreprise'.";
      case 4 :
        return "Code de l'erreur = 4 : Il y a une discordance dans l'exercice" +
        " entre le nom du fichier et la balise 'Exercice'.";
      case 5 :
        return "Code de l'erreur = 5 : Il y a une discordance dans la période" +
        " entre le nom du fichier et la balise 'Periode'.";
      case 6 :
        return "Code de l'erreur = 6 : Il y a une discordance dans le code" +
        " état entre le nom du fichier et la balise 'CodeEtat'.";
      case 7 :
        return "Code de l'erreur = 7 : Il y a une discordance dans le code" +
        " tableau entre le nom du fichier et la balise 'CodeTableau'.";
      case 8 :
        return "Code de l'erreur = 8 : Il y a une discordance dans le code" +
        " combinaison entre le nom du fichier et la balise 'CodeCombinaison'.";
      case 9 :
        return "Code de l'erreur = 9 : Le code entreprise de la balise" +
        " 'CodeEntreprise' n'est pas correct.";
      case 10 :
        return "Code de l'erreur = 10 : L'exercice de la balise 'Exercice'" +
        " n'est pas correct.";
      case 11 :
        return "Code de l'erreur = 11 : La période de la balise 'Periode'" +
        " n'est pas correct.";
      case 12 :
        return "Code de l'erreur = 12 : Le code état de la balise 'CodeEtat'" +
        " n'est pas correct.";
      case 13 :
        return "Code de l'erreur = 13 : Le code tableau de la balise" +
        " 'CodeTableau' n'est pas correct.";
      case 14 :
        return "Code de l'erreur = 14 : Le code combinaison de la balise" +
        " 'CodeCombinaison' n'est pas correct.";
      case 15 :
        return "Code de l'erreur = 15 : La période de la balise 'Periode'" +
        " est supérieure à la fréquence de réception.";
      case 16 :
        return "Code de l'erreur = 16 : L'exercice de la balise 'Exercice'" +
        " doit être égal au dernier exercice saisi + 1.";
      case 17 :
        return "Code de l'erreur = 17 : La période de la balise 'Periode'" +
        " doit être égale à 1.";
      case 18 :
        return "Code de l'erreur = 18 : L'exercice de la balise 'Exercice'" +
        " doit être égal au dernier exercice saisi.";
      case 19 :
        return "Code de l'erreur = 19 : La période de la balise 'Periode'" +
        " doit être égale à la dernière période saisie + 1.";
      case 20 :
        return "Code de l'erreur = 20 : Un des ordres lignes dans le fichier" +
        " XML est supérieur au nombre total des lignes de ce tableau.";
      case 21 :
        return "Code de l'erreur = 21 : Un des ordres colonnes dans le fichier" +
        " XML est supérieur au nombre total des colonnes de ce tableau.";
      case 22 :
        return "Code de l'erreur = 22 : Le nombre de cellules dans le fichier" +
        " XML est supérieur au nombre total des cellules" +
        " (hors cellules creuses) de ce tableau.";
      case 23 :
        return "Code de l'erreur = 23 : Une cellule qui doit être creuse a été" +
        " déclarée dans le fichier XML.";
      case 24 :
        return "Code de l'erreur = 24 : Erreur avec la base de données. Vérifier" +
        " le paramètrage du tableau concerné en utilisant les écrans de saisie.";
      case 25 :
        return "Code de l'erreur = 25 : Les données de ce fichier existent déjà" +
        " dans la base de données.";
      case 26 :
        return "Code de l'erreur = 26 : Un des ordres champs supplémentaire dans" +
        " le fichier XML est supérieur au nombre total des champs supplémentaire" +
        " de ce tableau.";
      case 27 :
        return "Code de l'erreur = 27 : Le nombre de champs supplémentaire dans" +
        " le fichier XML est supérieur au nombre total des champs supplémentaire" +
        " de ce tableau.";
    }
    return "";
  }
  private int fileNameWithXMLData(String fichier) {
    if (fichier.indexOf(".xml") == -1) {
      return 1;
    }
  // String file = fichier.substring(fichier.lastIndexOf("\\") + 1,fichier.indexOf(".xml"));
   String file = fichier.substring(fichier.lastIndexOf("\\")+ 1 ,fichier.indexOf(".xml"));   
   System.out.println("file fileNameWithXMLData  "+file);
   //String file = fichier.substring(fichier.lastIndexOf("\"),fichier.indexOf(".xml"));
   String[] codes = file.split("-");
    if (codes.length != 6) {
      System.out.println("----------     2");
      return 2;
    }
    if (!(codes[0].equals(document.getRootElement().getChild("Identification")
        .getChild("CodeEntreprise").getText()))) {
      System.out.println("-----------   3");
      return 3;
    }
    if (!(codes[1].equals(document.getRootElement().getChild("Identification")
        .getChild("Exercice").getText()))) {
      System.out.println("-----------   4");
      return 4;
    }
    if (!(codes[2].equals(document.getRootElement().getChild("Identification")
        .getChild("Periode").getText()))) {
      return 5;
    }
    if (!(codes[3].equals(document.getRootElement().getChild("Identification")
        .getChild("CodeEtat").getText()))) {
      return 6;
    }
    if (!(codes[4].equals(document.getRootElement().getChild("Identification")
        .getChild("CodeTableau").getText()))) {
      return 7;
    }
    if (!(codes[5].equals(document.getRootElement().getChild("Identification")
        .getChild("CodeCombinaison").getText()))) {
      return 8;
    }
    return 0;
  }
  private String version(String ver, String codeEntreprise, String codeEtat,
    String codeTableau, String codeExercice, String codeMois)
    throws SQLException {
    String max_exe = "";
    String max_moi = "";
    Statement stmt = con.createStatement();
    ResultSet rset = null;
    String sql = "";
    System.out.println("je suis dans version");
    if (ver.equals("E")) {
      System.out.println("je suis dans le cas de E");
      sql = "select exe_deb, moi_deb from eta_entreprise where exe_deb = (" +
        "select max(exe_deb) from eta_entreprise where cde_ent = '" +
        codeEntreprise + "' and cde_eta = '" + codeEtat +
        "') and moi_deb = (select max(moi_deb) from eta_entreprise where " +
        "cde_ent = '" + codeEntreprise + "' and cde_eta = '" + codeEtat +
        "' and exe_deb = (select max(exe_deb) from eta_entreprise where " +
        "cde_ent = '" + codeEntreprise + "' and cde_eta = '" + codeEtat +
        "')) and cde_ent = '" + codeEntreprise + "' and cde_eta = '" +
        codeEtat + "'";
      System.out.println("la requete est "+sql);
      rset = stmt.executeQuery(sql);
      if (rset.next()) {
        max_exe = rset.getString(1);
        max_moi = rset.getString(2);
      } else {
        rset.close();
        return null;
      }
      rset.close();
      System.out.println("avant integer paser");
      if ((Integer.parseInt(codeExercice) > Integer.parseInt(max_exe)) ||
        ((Integer.parseInt(codeExercice) == Integer.parseInt(max_exe)) &&
        (Integer.parseInt(codeMois) >= Integer.parseInt(max_moi)))) {
        sql = "select ver_eta from eta_entreprise where cde_eta = '" +
          codeEtat + "' and cde_ent = '" + codeEntreprise +
          "' and exe_deb = '" + max_exe + "' and moi_deb = '" + max_moi + "'";
        System.out.println("la requete est "+sql);
        rset = stmt.executeQuery(sql);
        if (rset.next()) {
          return rset.getString(1);
        }
        rset.close();
        return null;
      } else {
        sql = "select ver_eta from eta_entreprise where cde_eta = '" +
          codeEtat + "' and cde_ent = '" + codeEntreprise + "' and " +
          codeExercice + " between exe_deb and exe_fin and " + codeMois +
          " between moi_deb and moi_fin";
        System.out.println("la requete est "+sql);
        rset = stmt.executeQuery(sql);
        System.out.println("le resultat est version 4");
        if (rset.next()) {
          return rset.getString(1);
        }
        rset.close();
        System.out.println("apres fermeture");
        return null;
      }
    } else if (ver.equals("T")) {
      System.out.println("je suis dans le cas de T");
      sql = "select ver_tab from eta_version where cde_eta = '" + codeEtat +
        "' and cde_tab = '" + codeTableau + "' and ver_eta = '" +
        version("E", codeEntreprise, codeEtat, "", codeExercice, codeMois) +
        "'";
      System.out.println("la requete est "+sql);  
      rset = stmt.executeQuery(sql);
      if (rset.next()) {
        return rset.getString(1);
      }
      rset.close();
      return null;
    } else {
      return null;
    }
  }
}
