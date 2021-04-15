/*********************************************************************************
 * VARIABLES ET METHODES FOURNIES PAR LA CLASSE UtilLex (cf libClass_Projet)     *
 *       complement Ã  l'ANALYSEUR LEXICAL produit par ANTLR                      *
 *                                                                               *
 *                                                                               *
 *   nom du programme compile, sans suffixe : String UtilLex.nomSource           *
 *   ------------------------                                                    *
 *                                                                               *
 *   attributs lexicaux (selon items figurant dans la grammaire):                *
 *   ------------------                                                          *
 *     int UtilLex.valEnt = valeur du dernier nombre entier lu (item nbentier)   *
 *     int UtilLex.numIdCourant = code du dernier identificateur lu (item ident) *
 *                                                                               *
 *                                                                               *
 *   methodes utiles :                                                           *
 *   ---------------                                                             *
 *     void UtilLex.messErr(String m)  affichage de m et arret compilation       *
 *     String UtilLex.chaineIdent(int numId) delivre l'ident de codage numId     *
 *     void afftabSymb()  affiche la table des symboles                          *
 *********************************************************************************/

import java.io.*;

/**
 * classe de mise en oeuvre du compilateur
 * ======================================= (verifications semantiques +
 * production du code objet)
 * 
 * @author Girard, Grazon, Masson
 *
 */

public class PtGen {

	// constantes manipulees par le compilateur
	// ----------------------------------------

	private static final int

	// taille max de la table des symboles
	MAXSYMB = 300,

			// codes MAPILE :
			RESERVER = 1, EMPILER = 2, CONTENUG = 3, AFFECTERG = 4, OU = 5, ET = 6, NON = 7, INF = 8, INFEG = 9,
			SUP = 10, SUPEG = 11, EG = 12, DIFF = 13, ADD = 14, SOUS = 15, MUL = 16, DIV = 17, BSIFAUX = 18,
			BINCOND = 19, LIRENT = 20, LIREBOOL = 21, ECRENT = 22, ECRBOOL = 23, ARRET = 24, EMPILERADG = 25,
			EMPILERADL = 26, CONTENUL = 27, AFFECTERL = 28, APPEL = 29, RETOUR = 30,

			// codes des valeurs vrai/faux
			VRAI = 1, FAUX = 0,

			// types permis :
			ENT = 1, BOOL = 2, NEUTRE = 3,

			// categories possibles des identificateurs :
			CONSTANTE = 1, VARGLOBALE = 2, VARLOCALE = 3, PARAMFIXE = 4, PARAMMOD = 5, PROC = 6, DEF = 7, REF = 8,
			PRIVEE = 9,

			// valeurs possible du vecteur de translation
			TRANSDON = 1, TRANSCODE = 2, REFEXT = 3;

	private static int cptVarGlobales;
	private static int cptArgs;
	// utilitaires de controle de type
	// -------------------------------
	/**
	 * verification du type entier de l'expression en cours de compilation (arret de
	 * la compilation sinon)
	 */
	private static void verifEnt() {
		if (tCour != ENT)
			UtilLex.messErr("expression entiere attendue");
	}

	/**
	 * verification du type booleen de l'expression en cours de compilation (arret
	 * de la compilation sinon)
	 */
	private static void verifBool() {
		if (tCour != BOOL)
			UtilLex.messErr("expression booleenne attendue");
	}

	// pile pour gerer les chaines de reprise et les branchements en avant
	// -------------------------------------------------------------------

	private static TPileRep pileRep;

	// production du code objet en memoire
	// -----------------------------------

	private static ProgObjet po;

	// COMPILATION SEPAREE
	// -------------------
	//
	/**
	 * modification du vecteur de translation associe au code produit +
	 * incrementation attribut nbTransExt du descripteur NB: effectue uniquement si
	 * c'est une reference externe ou si on compile un module
	 * 
	 * @param valeur : TRANSDON, TRANSCODE ou REFEXT
	 */
	private static void modifVecteurTrans(int valeur) {
		if (valeur == REFEXT || desc.getUnite().equals("module")) {
			po.vecteurTrans(valeur);
			desc.incrNbTansExt();
		}
	}

	// descripteur associe a un programme objet (compilation separee)
	private static Descripteur desc;

	// autres variables fournies
	// -------------------------

	// MERCI de renseigner ici un nom pour le trinome, constitue EXCLUSIVEMENT DE
	// LETTRES
	public static String trinome = "CalonneLeMoan";

	private static int tCour; // type de l'expression compilee
	private static int vCour; // sert uniquement lors de la compilation d'une valeur (entiere ou boolenne)

	// TABLE DES SYMBOLES
	// ------------------
	//
	private static EltTabSymb[] tabSymb = new EltTabSymb[MAXSYMB + 1];

	// it = indice de remplissage de tabSymb
	// bc = bloc courant (=1 si le bloc courant est le programme principal)
	private static int it, bc;

	/**
	 * utilitaire de recherche de l'ident courant (ayant pour code
	 * UtilLex.numIdCourant) dans tabSymb
	 * 
	 * @param borneInf : recherche de l'indice it vers borneInf (=1 si recherche
	 *                 dans tout tabSymb)
	 * @return : indice de l'ident courant (de code UtilLex.numIdCourant) dans
	 *         tabSymb (O si absence)
	 */
	private static int presentIdent(int borneInf) {
		int i = it;
		while (i >= borneInf && tabSymb[i].code != UtilLex.numIdCourant)
			i--;
		if (i >= borneInf)
			return i;
		else
			return 0;
	}

	/**
	 * utilitaire de placement des caracteristiques d'un nouvel ident dans tabSymb
	 * 
	 * @param code : UtilLex.numIdCourant de l'ident
	 * @param cat  : categorie de l'ident parmi CONSTANTE, VARGLOBALE, PROC, etc.
	 * @param type : ENT, BOOL ou NEUTRE
	 * @param info : valeur pour une constante, ad d'execution pour une variable,
	 *             etc.
	 */
	private static void placeIdent(int code, int cat, int type, int info) {
		if (it == MAXSYMB)
			UtilLex.messErr("debordement de la table des symboles");
		it = it + 1;
		tabSymb[it] = new EltTabSymb(code, cat, type, info);
	}

	/**
	 * utilitaire d'affichage de la table des symboles
	 */
	private static void afftabSymb() {
		System.out.println("       code           categorie      type    info");
		System.out.println("      |--------------|--------------|-------|----");
		for (int i = 1; i <= it; i++) {
			if (i == bc) {
				System.out.print("bc=");
				Ecriture.ecrireInt(i, 3);
			} else if (i == it) {
				System.out.print("it=");
				Ecriture.ecrireInt(i, 3);
			} else
				Ecriture.ecrireInt(i, 6);
			if (tabSymb[i] == null)
				System.out.println(" reference NULL");
			else
				System.out.println(" " + tabSymb[i]);
		}
		System.out.println();
	}

	/**
	 * initialisations A COMPLETER SI BESOIN -------------------------------------
	 */
	public static void initialisations() {

		// indices de gestion de la table des symboles
		it = 0;
		bc = 1;

		// pile des reprises pour compilation des branchements en avant
		pileRep = new TPileRep();
		// programme objet = code Mapile de l'unite en cours de compilation
		po = new ProgObjet();
		// COMPILATION SEPAREE: desripteur de l'unite en cours de compilation
		desc = new Descripteur();

		// initialisation necessaire aux attributs lexicaux
		UtilLex.initialisation();

		// initialisation du type de l'expression courante
		tCour = NEUTRE;
		// initiatlisation du nombre de variables globales declarees
		cptVarGlobales = 0;

	} // initialisations

	/**
	 * code des points de generation A COMPLETER
	 * -----------------------------------------
	 * 
	 * @param numGen : numero du point de generation a executer
	 */
	public static void pt(int numGen) {

		switch (numGen) {
		case 0:
			initialisations();
			break;

		case 1: // consts
			if (presentIdent(1) == 0) {
				placeIdent(UtilLex.numIdCourant, CONSTANTE, tCour, vCour);
			} else {
				UtilLex.messErr("Constante deja declaree.");
			}
			break;

		case 2: // vars
			if (presentIdent(1) == 0) {
				if (bc == 1) {
					placeIdent(UtilLex.numIdCourant, VARGLOBALE, tCour, vCour);
					cptVarGlobales++;
				}else {
					placeIdent(UtilLex.numIdCourant, VARLOCALE, tCour, vCour);
				}
			} else {
				UtilLex.messErr("Variable deja declaree");
			}
			break;

		// Lecture de valeur
		case 37 :
			tCour = ENT;
			break;
		case 3: // '-' nbentier
			vCour = (-1) * vCour;
			tCour = ENT;
			break;
		case 4: // "vrai"
			vCour = 1;
			tCour = BOOL;
			break;
		case 5:
			vCour = 0;
			tCour = BOOL;
			break;
			
		// Lecture du type
		case 6:
			tCour = ENT;
			break;
		case 7:
			tCour = BOOL;
			break;

		// declaration
		case 8:
			po.produire(RESERVER);
			po.produire(cptVarGlobales);
			break;
		// lecture
		case 9:
			if (tCour == ENT) {
				po.produire(LIRENT);
			} else if (tCour == BOOL) {
				po.produire(LIREBOOL);
			}
			break;

		// ecriture
		case 10:
			if (tCour == ENT) {
				po.produire(ECRENT);
			} else if (tCour == BOOL) {
				po.produire(ECRBOOL);
			}
			break;
		// expression

		// Les case 11 et 15 sont utilises pour verifier que l'expression est bien de
		// type booleenne ou entiere

		case 11:
			verifBool();
			break;

		case 15:
			verifEnt();
			break;

		// Production des operations entre expressions
		case 12:
			po.produire(OU);
			break;

		case 13:
			po.produire(ET);
			break;

		case 14:
			po.produire(NON);
			break;

		case 16:
			po.produire(EG);
			tCour = BOOL;
			break;

		case 17:
			po.produire(DIFF);
			tCour = BOOL;
			break;

		case 18:
			po.produire(SUP);
			tCour = BOOL;
			break;

		case 19:
			po.produire(SUPEG);
			tCour = BOOL;
			break;

		case 20:
			po.produire(INF);
			tCour = BOOL;
			break;

		case 21:
			po.produire(INFEG);
			tCour = BOOL;
			break;

		case 22:
			po.produire(ADD);
			break;

		case 23:
			po.produire(DIFF);
			break;

		case 24:
			po.produire(MUL);
			break;

		case 25:
			po.produire(DIV);
			break;

		// Affectation de la valeur d'une expression a une variable
		case 34:
			if (presentIdent(1) == 0) {
				UtilLex.messErr("ident non present");
			} else {
				int identCat = tabSymb[UtilLex.numIdCourant].categorie;
				if (identCat == CONSTANTE || identCat == PARAMFIXE) {
					UtilLex.messErr(
							"ident non modifiable. L'ident ne doit pas �tre une constante ou un parametre fixe");
				}
				if (identCat == VARGLOBALE) {
					po.produire(AFFECTERG);
					po.produire(tabSymb[UtilLex.numIdCourant].code);
				}
				else if(identCat == VARLOCALE || identCat == PARAMMOD) {
					po.produire(AFFECTERL);
					po.produire(tabSymb[UtilLex.numIdCourant].code);
					if (identCat == VARLOCALE)
						po.produire(0);
					if (identCat == PARAMMOD)
						po.produire(1);
				}
			}

		// Gestion de primaire
		case 26: // valeur
			po.produire(EMPILER);
			break;

		case 27: // ident
			// On verifie que l'ident lu existe dans la table
			int id_ident = presentIdent(1);
			if (id_ident == 0) {
				UtilLex.messErr("L'ident utilise n'existe pas");
			}
			// On change le type courant
			tCour = tabSymb[id_ident].type;
			// On trouve le type de l'ident (variable locale, globale ou constante)
			switch (tabSymb[id_ident].categorie) {

			case CONSTANTE:
				po.produire(EMPILER);
				po.produire(tabSymb[id_ident].code);
				break;

			case VARGLOBALE:
				po.produire(CONTENUG);
				po.produire(tabSymb[id_ident].code);
				break;

			case VARLOCALE:
				po.produire(CONTENUL);
				po.produire(tabSymb[id_ident].code);
				po.produire(0);
				
			case PARAMMOD:
				po.produire(CONTENUL);
				po.produire(tabSymb[id_ident].code);
				po.produire(1);
			

			}

			// Branchements conditionnels

			// Evaluation d'une expression
		case 28:
			// Produire un bsifaux
			po.produire(BSIFAUX);
			pileRep.empiler(po.getIpo());
			break;

		// "sinon"
		case 29:
			int ipo_si = pileRep.depiler();
			po.produire(BINCOND);
			pileRep.empiler(po.getIpo());
			po.modifier(ipo_si, po.getIpo() + 1);
			break;

		// "fsi"
		case 30:
			int dernier_branch = pileRep.depiler();
			po.modifier(dernier_branch, po.getIpo() + 1);
			break;

		// Boucle "ttq"
		case 31:
			pileRep.empiler(po.getIpo() + 1);
			break;

		// Fin Tant que

		case 32:
			int sortie_ttq = pileRep.depiler(); // Valeur d'ipo pour sortir du ttq (empil�e au case 28)
			int debut_ttq = pileRep.depiler(); // Valeur d'ipo pour reboucler (empil�e au case 30)
			po.produire(BINCOND);
			po.produire(debut_ttq);

			po.modifier(sortie_ttq, po.getIpo() + 1);
			break;
		
		// Debut cond
			
		// Creation du premier BSIFAUX au debut du cond
		case 33:
			po.produire(BSIFAUX);
			po.produire(0); // valeur bidon a changer plus tard
			pileRep.empiler(po.getIpo());
			break;
		
		// Rebanchement du BSIFAUX et production du BINCOND
		case 35 :
			po.produire(BINCOND); // On produit un BINCOND au cas ou le BSIFAUX precedent n'a pas ete pris
			po.produire(0);
		
			pileRep.empiler(po.getIpo()); // On empile l'ipo pour modifier la valeur du BINCOND plus tard
			
			int ipo_bsifaux_prec = pileRep.depiler(); // ipo du BSIFAUX precedent dont on doit modifier la valeur
			po.modifier(ipo_bsifaux_prec, po.getIpo() + 1);
			
			break;
		
		// Gestion du 'aut'
		case 38 :
			int ipo_bsifaux = pileRep.depiler(); // ipo du BSIFAUX precedent dont on doit modifier la valeur
			po.modifier(ipo_bsifaux, po.getIpo() + 1);
			
			break;
			
		// fincond, rebranchement des BINCOND
		case 36 : 
			int ipo_bincond = pileRep.depiler(); // ipo du bincond  a rebrancher
			int ipo_fincond = po.getIpo() + 1; // ipo auquel brancher le bincond
			while(ipo_bincond == 0) {
				po.modifier(ipo_bincond, ipo_fincond);
				ipo_bincond = pileRep.depiler();
			}
			break;
		// Declaration des Procédures
		case 39 :
            if (presentIdent(1) == 0) {
                placeIdent(UtilLex.numIdCourant, PROC, NEUTRE, po.getIpo() + 1);
                placeIdent(-1, PRIVEE, NEUTRE, 0);
                bc = it + 1;
                cptArgs = 0;
            }
            else UtilLex.messErr("La procedure a deja ete declaree !");
            break;

        case 40 :
            if (presentIdent(bc) == 0) {
                placeIdent(UtilLex.numIdCourant, PARAMFIXE, tCour, cptArgs);
                cptArgs++;
            }
            else UtilLex.messErr("Le parametre fixe a deja ete declare.");
            break;

        case 41:
            if (presentIdent(bc) == 0) {
                placeIdent(UtilLex.numIdCourant, PARAMMOD, tCour, cptArgs);
                cptArgs++;
            }
            else UtilLex.messErr("Le parametre modifiable a deja ete declare.");
            break;

        case 42:
            EltTabSymb ligneParams = tabSymb[bc - 1];
            ligneParams.info = cptArgs;
            break;
            
        case 43:

            po.produire(RETOUR);
            po.produire(cptArgs);

            it = bc + cptArgs - 1;

            for (int i = it ; i >= bc ; i--) {
                tabSymb[i].code = -1;
            }

            bc = 1;
            break;
           
        case 44:
        	int id_ident_cat = tabSymb[UtilLex.numIdCourant].categorie;
        	int ident_adr = tabSymb[UtilLex.numIdCourant].code;
        	switch(id_ident_cat) {
        	
	        	case VARGLOBALE:
	        		po.produire(EMPILERADG);
	        		po.produire(ident_adr);
	        		break;
	        		
	        	case VARLOCALE:
	        		po.produire(EMPILERADL);
	        		po.produire(ident_adr);
	        		po.produire(0);
	        		break;
	        	
	        	case PARAMMOD:
	        		po.produire(EMPILERADL);
	        		po.produire(ident_adr);
	        		po.produire(1);
	        		break;
        	}
        	
        	break;
        	
		case 255:
			afftabSymb(); // affichage de la table des symboles en fin de compilation
			break;
			
		default:
			System.out.println("Point de generation non prevu dans votre liste");
			break;

		}
	}
}