package com.software.portal.mymarks;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.widget.Toast;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {
        // UI references.
    private EditText mSNumberView;
    private EditText mPasswordView;
    private CheckBox mSaveView;
    private View mProgressView;
    private View mLoginFormView;
    public static final String PREFS_NAME = "file";
    private static final String PREF_USERNAME = "username";
    private static final String PREF_PASSWORD = "password";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.
        mSNumberView = (EditText) findViewById(R.id.sNumber);
        mSaveView = (CheckBox) findViewById(R.id.checkRememeber);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        SharedPreferences pref = getSharedPreferences(PREFS_NAME,MODE_PRIVATE);
        String username = pref.getString(PREF_USERNAME, null);
        String password = pref.getString(PREF_PASSWORD, null);

        if (username!=null && password!=null) {
            mSaveView.setChecked(true);
            mSNumberView.setText(username);
            mPasswordView.setText(password);
        }

        Button mEmailSignInButton = (Button) findViewById(R.id.portal_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        // Reset errors.
        mSNumberView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        final String sNumber = mSNumberView.getText().toString();
        final String password = mPasswordView.getText().toString();

        if (mSaveView.isChecked()) {
        /*Store username and password for future use*/
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putString(PREF_USERNAME, sNumber)
                    .putString(PREF_PASSWORD, password)
                    .commit();
        } else {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .remove(PREF_USERNAME)
                    .remove(PREF_PASSWORD)
                    .commit();
        }

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError("This field is required");
            focusView = mPasswordView;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            mPasswordView.setError("Password must contain at least 4 characters.");
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid student number address.
        if (TextUtils.isEmpty(sNumber)) {
            mSNumberView.setError(getString(R.string.error_field_required));
            focusView = mSNumberView;
            cancel = true;
        } else if (!isEmailValid(sNumber)) {
            mSNumberView.setError("Incorrect student number format.");
            focusView = mSNumberView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);

            new GetMarksTask(sNumber, password).execute();
        }
    }

    private boolean isEmailValid(String email) {
        return email.matches("u\\d{8}+");
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private class GetMarksTask extends AsyncTask<Void, String, String> {
        private final String sNumber;
        private final String pwd;
        private UPPortal portal;
        private String XML = "<?xml version='1.0' encoding='utf-8' ?>" +
                "<PAGE id='UP_SS_STUD'><GENSCRIPT id='script'><![CDATA[var oWin=window;" +
                " var oDoc = window.document;" +
                "oWin.PIA_KEYSTRUCT={EMPLID:\"13108710\"};" +
                " try {" +
                " var tframe = top.frames['ptcxmiframe'] || top.frames['NAV'];" +
                " if (tframe && !isCrossDomain(tframe))" +
                " { if (tframe.ptcxmProcess)" +
                "  { tframe.ptcxmProcess(); }" +
                "   else if (tframe.epplnProcess)" +
                " { tframe.epplnProcess(); }" +
                "}" +
                "  } catch (ex) {}" +
                " oWin.strCurrUrl='https://upnet.up.ac.za/psp/pscsmpra/EMPLOYEE/HRMS/c/UP_SS_MENU.UP_SS_STUDENT.GBL?EMPLID=13108710&PAGE=UP_SS_STUD';" +
                "]]></GENSCRIPT><GENSCRIPT id='script'><![CDATA[oWin.gridRowSelRgbColor_win0 ='rgb(238,238,238)';" +
                "]]></GENSCRIPT><GENSCRIPT id='onloadScript'><![CDATA[var pt_pageinfo = document.getElementById('pt_pageinfo_win0');" +
                "if (pt_pageinfo) {" +
                "pt_pageinfo.setAttribute('Page', 'UP_SS_STUD_SR_SP');" +
                "pt_pageinfo.setAttribute('Component', 'UP_SS_STUDENT');" +
                "pt_pageinfo.setAttribute('Menu', 'UP_SS_MENU');" +
                "pt_pageinfo.setAttribute('Mode', 'CLASSIC');" +
                "}" +
                "g_bAccessibilityMode=false;" +
                "var actn='';" +
                "var oWin=window;" +
                "var oDoc=document;" +
                "actn=oDoc.win0.ICAction.value;" +
                "oDoc.win0.ICAction.value='None';" +
                "oDoc.win0.ICResubmit.value='0';" +
                "oWin.nResubmit='0';" +
                "oDoc.win0.ICStateNum.value=4;initVars_win0();" +
                "resetVars_win0();" +
                "document.win0.ICChanged.value='0';" +
                "oDoc.win0.ICFocus.value='';" +
                "setupTimeout2();" +
                "ptEvent.add(window,'scroll',positionWAIT_win0);" +
                "ptCommonObj2.generateABNSearchResults(document.win0);" +
                "getGblSrchPageNum(actn);" +
                "if (gSrchRsltPageNum <= 5) getAllRelatedActions();" +
                "" +
                "if (typeof(myAppsWindowOpenJS) != 'undefined' && myAppsWindowOpenJS != null && myAppsWindowOpenJS != '')" +
                " {" +
                "try {eval(myAppsWindowOpenJS);} catch(e) {}" +
                "  myAppsWindowOpenJS=null;" +
                "}" +
                "if (typeof(ptLongEditCounter) != 'undefined' && ptLongEditCounter != null)" +
                "   ptLongEditCounter.onLoadLongEditCounter();" +
                "if (typeof(HelppopupObj_win0) != 'undefined' && HelppopupObj_win0 != null)" +
                " HelppopupObj_win0.StopPopup('win0');" +
                "doModalOnLoad_win0(false, false, true);" +
                "ResetGlyph_win0();" +
                "self.scroll(0,0);" +
                "objToBeFocus = null;" +
                "if (typeof oWin == 'undefined') setEventHandlers_win0('ICFirstAnchor_win0', 'ICLastAnchor_win0', false);" +
                " else" +
                " oWin.setEventHandlers_win0('ICFirstAnchor_win0', 'ICLastAnchor_win0', false);" +
                "setFocus_win0('CLASS_NAME1$0',-1);" +
                "ptLoadingStatus_empty(0);" +
                "setupTimeout2();" +
                "processing_win0(0,3000);]]></GENSCRIPT>" +
                "<FIELD id='win0divPSHIDDENFIELDS'><![CDATA[<input type='hidden' name='ICType' id='ICType' value='Panel' />" +
                "<input type='hidden' name='ICElementNum' id='ICElementNum' value='0' />" +
                "<input type='hidden' name='ICStateNum' id='ICStateNum' value='4' />" +
                "<input type='hidden' name='ICAction' id='ICAction' value='None' />" +
                "<input type='hidden' name='ICXPos' id='ICXPos' value='0' />" +
                "<input type='hidden' name='ICYPos' id='ICYPos' value='0' />" +
                "<input type='hidden' name='ResponsetoDiffFrame' id='ResponsetoDiffFrame' value='-1' />" +
                "<input type='hidden' name='TargetFrameName' id='TargetFrameName' value='None' />" +
                "<input type='hidden' name='FacetPath' id='FacetPath' value='None' />" +
                "<input type='hidden' name='ICFocus' id='ICFocus' value='' />" +
                "<input type='hidden' name='ICSaveWarningFilter' id='ICSaveWarningFilter' value='0' />" +
                "<input type='hidden' name='ICChanged' id='ICChanged' value='0' />" +
                "<input type='hidden' name='ICAutoSave' id='ICAutoSave' value='0' />" +
                "<input type='hidden' name='ICResubmit' id='ICResubmit' value='0' />" +
                "<input type='hidden' name='ICSID' id='ICSID' value='ZDLScNDHVlNiBH3wYXc1LP8t3GgJn16wMtx4oui2GY8=' />" +
                "<input type='hidden' name='ICActionPrompt' id='ICActionPrompt' value='false' />" +
                "<input type='hidden' name='ICBcDomData' id='ICBcDomData' value='' />" +
                "<input type='hidden' name='ICPanelName' id='ICPanelName' value='' />" +
                "<input type='hidden' name='ICFind' id='ICFind' value='' />" +
                "<input type='hidden' name='ICAddCount' id='ICAddCount' value='' />" +
                "<input type='hidden' name='ICAPPCLSDATA' id='ICAPPCLSDATA' value='' />" +
                "]]></FIELD>" +
                "<FIELD id='win0divPAGEBAR'><![CDATA[<DIV><table cols='3' width='100%' cellpadding='0' cellspacing='0' hspace='0' vspace='0'>" +
                "<tr>" +
                "<td width='80%'></td><td width='10%' nowrap='nowrap' align='right'></td>" +
                "<td width='10%' nowrap='nowrap' align='right'><a id='NEWWIN' name='NEWWIN' class='PSHYPERLINK' dir='ltr'href=\"javascript:processing_win0(0,3000); void window.open(DoPortalUrl('https://upnet.up.ac.za/psp/pscsmpra_newwin/EMPLOYEE/HRMS/c/UP_SS_MENU.UP_SS_STUDENT.GBL'),'','');\" PSaccesskey='9' tabindex='1' class='PSHYPERLINK' >New Window</a>&nbsp;|&nbsp;<a id='HELP' name='HELP' class='PSHYPERLINK' href=\"javascript:void window.open('http://docs.oracle.com/cd/E56917_01/cs9pbr4/f1search.htm?ContextID=UP_SS_STUD_SR_SP&LangCD=ENG','help','');\" tabindex='2' class='PSHYPERLINK' >Help</a>&nbsp;|&nbsp;<a href=\"javascript:submitAction_win0(document.win0,'#ICCustPage');\" id=CUSTPAGE class='PSHYPERLINK' tabindex='3' dir='ltr' class='PSHYPERLINK' >Personalize Page</a></td></tr>" +
                "</table>" +
                "</DIV>]]></FIELD><GENSCRIPT id='onloadScript'><![CDATA[if (typeof window.top.ptrc != \"undefined\" && window.top.ptrc != null){window.top.ptrc.SetRcEnabled(false);window.top.ptrc.initRC();}]]></GENSCRIPT>" +
                "<FIELD id='win0divPSPANELTABS'><![CDATA[]]></FIELD>" +
                "<FIELD id='win0divPAGECONTAINER'><![CDATA[<DIV class='ps_pspagecontainer' id='win0divPSPAGECONTAINER'><table role='presentation'  border='0' id='ACE_width' cellpadding='0' cellspacing='0' class='PSPAGECONTAINER' cols='9' width='719'>" +
                "<tr>" +
                "<td width='1' height='17'></td>" +
                "<td width='7'></td>" +
                "<td width='13'></td>" +
                "<td width='44'></td>" +
                "<td width='8'></td>" +
                "<td width='360'></td>" +
                "<td width='68'></td>" +
                "<td width='8'></td>" +
                "<td width='210'></td>" +
                "</tr>" +
                "<tr>" +
                "<td height='32' colspan='3'></td>" +
                "<td colspan='3'  valign='top' align='left'>" +
                "<DIV    id='win0divHCR_PERSON_NM_I_NAME_DISPLAY'><span    aria-disabled='true' class='PSEDITBOX_DISPONLY' id='HCR_PERSON_NM_I_NAME_DISPLAY'>Noko Rammutla</span>" +
                "</DIV></td>" +
                "<td  valign='top' align='right'>" +
                "<DIV   id='win0divDERIVED_SSS_SCL_EMPLIDlbl'><span  class='PSEDITBOXLABEL' >Empl ID:</span> </DIV></td>" +
                "<td></td>" +
                "<td  valign='top' align='left'>" +
                "<DIV    id='win0divDERIVED_SSS_SCL_EMPLID'><span    aria-disabled='true' class='PSEDITBOX_DISPONLY' id='DERIVED_SSS_SCL_EMPLID'>13108710</span>" +
                "</DIV></td>" +
                "</tr>" +
                "<tr>" +
                "<td height='22'></td>" +
                "<td colspan='3'  valign='top' align='right'>" +
                "<DIV   id='win0divUP_DERIVED_SS_STRM_1lbl'><span  class='PSEDITBOXLABEL' >Term:</span> </DIV></td>" +
                "<td></td>" +
                "<td colspan='4'  valign='top' align='left'>" +
                "<DIV    id='win0divUP_DERIVED_SS_STRM_1'><span    aria-disabled='true' class='PSEDITBOX_DISPONLY' id='UP_DERIVED_SS_STRM_1'>2016</span>" +
                "</DIV></td>" +
                "</tr>" +
                "<tr>" +
                "<td height='240' colspan='2'></td>" +
                "<td colspan='7'  valign='top' align='left'>" +
                "<DIV    id='win0divSTDNT_WEEK_SCHD$0'>" +
                "<table border='1' cellspacing='0' class='PSLEVEL1GRIDWBO'  id='STDNT_WEEK_SCHD$scroll$0' dir='ltr' cols='6' width='712' cellpadding='2'>" +
                "<tr><td class='PSLEVEL3GRIDLABEL'  colspan='6' align='left'><DIV    id='win0divSTDNT_WEEK_SCHDGP$0'>Current term enrollments</DIV></td></tr>" +
                "<tr>" +
                "<th scope='col' abbr='Course (Class nbr)' width='192' align='CENTER' class='PSLEVEL3GRIDCOLUMNHDR PSGRIDFIRSTCOLUMN' ><a name='STDNT_WEEK_SCHD$srt2$0' id='STDNT_WEEK_SCHD$srt2$0' tabindex='27' class='PSLEVEL3GRIDCOLUMNHDR' href=\"javascript:submitAction_win0(document.win0,'STDNT_WEEK_SCHD$srt2$0');\" title=\"Click column heading to sort ascending\">Course (Class nbr)</a></th>" +
                "<th scope='col' abbr='Description' width='15' align='left' class='PSLEVEL3GRIDCOLUMNHDR' ><a name='STDNT_WEEK_SCHD$srt3$0' id='STDNT_WEEK_SCHD$srt3$0' tabindex='28' class='PSLEVEL3GRIDCOLUMNHDR' href=\"javascript:submitAction_win0(document.win0,'STDNT_WEEK_SCHD$srt3$0');\" title=\"Click column heading to sort ascending\">Description</a></th>" +
                "<th scope='col' abbr='Session' width='43' align='left' class='PSLEVEL3GRIDCOLUMNHDR' ><a name='STDNT_WEEK_SCHD$srt4$0' id='STDNT_WEEK_SCHD$srt4$0' tabindex='29' class='PSLEVEL3GRIDCOLUMNHDR' href=\"javascript:submitAction_win0(document.win0,'STDNT_WEEK_SCHD$srt4$0');\" title=\"Click column heading to sort ascending\">Session</a></th>" +
                "<th scope='col' abbr='Progress Mark' width='43' align='CENTER' class='PSLEVEL3GRIDCOLUMNHDR' ><a name='STDNT_WEEK_SCHD$srt5$0' id='STDNT_WEEK_SCHD$srt5$0' tabindex='30' class='PSLEVEL3GRIDCOLUMNHDR' href=\"javascript:submitAction_win0(document.win0,'STDNT_WEEK_SCHD$srt5$0');\" title=\"Click column heading to sort ascending\">Progress Mark</a></th>" +
                "<th scope='col' abbr='Grade' width='49' align='left' class='PSLEVEL3GRIDCOLUMNHDR' ><a name='STDNT_WEEK_SCHD$srt6$0' id='STDNT_WEEK_SCHD$srt6$0' tabindex='31' class='PSLEVEL3GRIDCOLUMNHDR' href=\"javascript:submitAction_win0(document.win0,'STDNT_WEEK_SCHD$srt6$0');\" title=\"Click column heading to sort ascending\">Grade</a></th>" +
                "<th scope='col' abbr='Grade Description' width='53' align='left' class='PSLEVEL3GRIDCOLUMNHDR' ><a name='STDNT_WEEK_SCHD$srt7$0' id='STDNT_WEEK_SCHD$srt7$0' tabindex='32' class='PSLEVEL3GRIDCOLUMNHDR' href=\"javascript:submitAction_win0(document.win0,'STDNT_WEEK_SCHD$srt7$0');\" title=\"Click column heading to sort ascending\">Grade Description</a></th>" +
                "</tr>" +
                "<tr id='trSTDNT_WEEK_SCHD$0_row1' valign='center' onClick=\"HighLightTR('rgb(238,238,238)','','trSTDNT_WEEK_SCHD$0_row1');\" onMouseOver=\"hoverLightTR('rgb(253,255,200)','',0,'trSTDNT_WEEK_SCHD$0_row1');\" onmouseout=\"hoverLightTR('rgb(253,255,200)','',1,'trSTDNT_WEEK_SCHD$0_row1');\">" +
                "<td align='left'  height='22' class='PSLEVEL3GRID PSGRIDFIRSTCOLUMN' >" +
                "<DIV    id='win0divCLASS_NAME$0'><span id='CLASS_NAME$span$0'  class='PSHYPERLINKDISABLED'  title='View Details' >EAS 410-E100<br />" +
                "EXM (2433)</span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divCLASS_NAME1$0'><span id='CLASS_NAME1$span$0'  class='PSHYPERLINK'  title='Schedule' ><a name='CLASS_NAME1$0' id='CLASS_NAME1$0'  ptlinktgt='pt_peoplecode' tabindex='37' onclick='javascript:cancelBubble(event);' href=\"javascript:submitAction_win0(document.win0,'CLASS_NAME1$0');\"  class='PSHYPERLINK' >Computer engineering 410</a></span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divDERIVED_SSS_SCL_SESSION_CODE$0'><span    aria-disabled='true' class='PSDROPDOWNLIST_DISPONLY' id='DERIVED_SSS_SCL_SESSION_CODE$0'>First Semester</span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divOFFGRADE$0'><span    class='PSLONGEDITBOX' id='OFFGRADE$0'>&nbsp;</span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divDERIVED_SSS_SCL_UP_GRADE1$0'><span    aria-disabled='true' class='PSEDITBOX_DISPONLY' id='DERIVED_SSS_SCL_UP_GRADE1$0'>&nbsp;</span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divDERIVED_SSS_SCL_UP_GRANTOR_DESCR$0'><span    aria-disabled='true' class='PSEDITBOX_DISPONLY' id='DERIVED_SSS_SCL_UP_GRANTOR_DESCR$0'>Grade Outstanding</span>" +
                "</DIV></td>" +
                "</tr>" +
                "<tr id='trSTDNT_WEEK_SCHD$0_row2' valign='center' onClick=\"HighLightTR('rgb(238,238,238)','','trSTDNT_WEEK_SCHD$0_row2');\" onMouseOver=\"hoverLightTR('rgb(253,255,200)','',0,'trSTDNT_WEEK_SCHD$0_row2');\" onmouseout=\"hoverLightTR('rgb(253,255,200)','',1,'trSTDNT_WEEK_SCHD$0_row2');\">" +
                "<td align='left'  height='22' class='PSLEVEL3GRID PSGRIDFIRSTCOLUMN' >" +
                "<DIV    id='win0divCLASS_NAME$1'><span id='CLASS_NAME$span$1'  class='PSHYPERLINKDISABLED'  title='View Details' >EHN 410-E100<br />" +
                "EXM (2445)</span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divCLASS_NAME1$1'><span id='CLASS_NAME1$span$1'  class='PSHYPERLINK'  title='Schedule' ><a name='CLASS_NAME1$1' id='CLASS_NAME1$1'  ptlinktgt='pt_peoplecode' tabindex='43' onclick='javascript:cancelBubble(event);' href=\"javascript:submitAction_win0(document.win0,'CLASS_NAME1$1');\"  class='PSHYPERLINK' >e-Bus and network sec 410</a></span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divDERIVED_SSS_SCL_SESSION_CODE$1'><span    aria-disabled='true' class='PSDROPDOWNLIST_DISPONLY' id='DERIVED_SSS_SCL_SESSION_CODE$1'>First Semester</span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divOFFGRADE$1'><span    class='PSLONGEDITBOX' id='OFFGRADE$1'>&nbsp;</span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divDERIVED_SSS_SCL_UP_GRADE1$1'><span    aria-disabled='true' class='PSEDITBOX_DISPONLY' id='DERIVED_SSS_SCL_UP_GRADE1$1'>&nbsp;</span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divDERIVED_SSS_SCL_UP_GRANTOR_DESCR$1'><span    aria-disabled='true' class='PSEDITBOX_DISPONLY' id='DERIVED_SSS_SCL_UP_GRANTOR_DESCR$1'>Grade Outstanding</span>" +
                "</DIV></td>" +
                "</tr>" +
                "<tr id='trSTDNT_WEEK_SCHD$0_row3' valign='center' onClick=\"HighLightTR('rgb(238,238,238)','','trSTDNT_WEEK_SCHD$0_row3');\" onMouseOver=\"hoverLightTR('rgb(253,255,200)','',0,'trSTDNT_WEEK_SCHD$0_row3');\" onmouseout=\"hoverLightTR('rgb(253,255,200)','',1,'trSTDNT_WEEK_SCHD$0_row3');\">" +
                "<td align='left'  height='22' class='PSLEVEL3GRID PSGRIDFIRSTCOLUMN' >" +
                "<DIV    id='win0divCLASS_NAME$2'><span id='CLASS_NAME$span$2'  class='PSHYPERLINKDISABLED'  title='View Details' >EPR 402-E100<br />" +
                "EXM (2484)</span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divCLASS_NAME1$2'><span id='CLASS_NAME1$span$2'  class='PSHYPERLINK'  title='Schedule' ><a name='CLASS_NAME1$2' id='CLASS_NAME1$2'  ptlinktgt='pt_peoplecode' tabindex='49' onclick='javascript:cancelBubble(event);' href=\"javascript:submitAction_win0(document.win0,'CLASS_NAME1$2');\"  class='PSHYPERLINK' >Project 402</a></span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divDERIVED_SSS_SCL_SESSION_CODE$2'><span    aria-disabled='true' class='PSDROPDOWNLIST_DISPONLY' id='DERIVED_SSS_SCL_SESSION_CODE$2'>Year</span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divOFFGRADE$2'><span    class='PSLONGEDITBOX' id='OFFGRADE$2'>&nbsp;</span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divDERIVED_SSS_SCL_UP_GRADE1$2'><span    aria-disabled='true' class='PSEDITBOX_DISPONLY' id='DERIVED_SSS_SCL_UP_GRADE1$2'>12</span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divDERIVED_SSS_SCL_UP_GRANTOR_DESCR$2'><span    aria-disabled='true' class='PSEDITBOX_DISPONLY' id='DERIVED_SSS_SCL_UP_GRANTOR_DESCR$2'>Fail</span>" +
                "</DIV></td>" +
                "</tr>" +
                "<tr id='trSTDNT_WEEK_SCHD$0_row4' valign='center' onClick=\"HighLightTR('rgb(238,238,238)','','trSTDNT_WEEK_SCHD$0_row4');\" onMouseOver=\"hoverLightTR('rgb(253,255,200)','',0,'trSTDNT_WEEK_SCHD$0_row4');\" onmouseout=\"hoverLightTR('rgb(253,255,200)','',1,'trSTDNT_WEEK_SCHD$0_row4');\">" +
                "<td align='left'  height='22' class='PSLEVEL3GRID PSGRIDFIRSTCOLUMN' >" +
                "<DIV    id='win0divCLASS_NAME$3'><span id='CLASS_NAME$span$3'  class='PSHYPERLINKDISABLED'  title='View Details' >EPY 423-E100<br />" +
                "PRA (2330)</span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divCLASS_NAME1$3'><span id='CLASS_NAME1$span$3'  class='PSHYPERLINK'  title='Schedule' ><a name='CLASS_NAME1$3' id='CLASS_NAME1$3'  ptlinktgt='pt_peoplecode' tabindex='55' onclick='javascript:cancelBubble(event);' href=\"javascript:submitAction_win0(document.win0,'CLASS_NAME1$3');\"  class='PSHYPERLINK' >Prac training and report 423</a></span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divDERIVED_SSS_SCL_SESSION_CODE$3'><span    aria-disabled='true' class='PSDROPDOWNLIST_DISPONLY' id='DERIVED_SSS_SCL_SESSION_CODE$3'>Second Semester</span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divOFFGRADE$3'><span    class='PSLONGEDITBOX' id='OFFGRADE$3'>&nbsp;</span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divDERIVED_SSS_SCL_UP_GRADE1$3'><span    aria-disabled='true' class='PSEDITBOX_DISPONLY' id='DERIVED_SSS_SCL_UP_GRADE1$3'>75</span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divDERIVED_SSS_SCL_UP_GRANTOR_DESCR$3'><span    aria-disabled='true' class='PSEDITBOX_DISPONLY' id='DERIVED_SSS_SCL_UP_GRANTOR_DESCR$3'>Pass with distinction</span>" +
                "</DIV></td>" +
                "</tr>" +
                "<tr id='trSTDNT_WEEK_SCHD$0_row5' valign='center' onClick=\"HighLightTR('rgb(238,238,238)','','trSTDNT_WEEK_SCHD$0_row5');\" onMouseOver=\"hoverLightTR('rgb(253,255,200)','',0,'trSTDNT_WEEK_SCHD$0_row5');\" onmouseout=\"hoverLightTR('rgb(253,255,200)','',1,'trSTDNT_WEEK_SCHD$0_row5');\">" +
                "<td align='left'  height='22' class='PSLEVEL3GRID PSGRIDFIRSTCOLUMN' >" +
                "<DIV    id='win0divCLASS_NAME$4'><span id='CLASS_NAME$span$4'  class='PSHYPERLINKDISABLED'  title='View Details' >ERP 420-E100<br />" +
                "EXM (2332)</span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divCLASS_NAME1$4'><span id='CLASS_NAME1$span$4'  class='PSHYPERLINK'  title='Schedule' ><a name='CLASS_NAME1$4' id='CLASS_NAME1$4'  ptlinktgt='pt_peoplecode' tabindex='61' onclick='javascript:cancelBubble(event);' href=\"javascript:submitAction_win0(document.win0,'CLASS_NAME1$4');\"  class='PSHYPERLINK' >Specialisation 420</a></span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divDERIVED_SSS_SCL_SESSION_CODE$4'><span    aria-disabled='true' class='PSDROPDOWNLIST_DISPONLY' id='DERIVED_SSS_SCL_SESSION_CODE$4'>Second Semester</span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divOFFGRADE$4'><span    class='PSLONGEDITBOX' id='OFFGRADE$4'>&nbsp;</span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divDERIVED_SSS_SCL_UP_GRADE1$4'><span    aria-disabled='true' class='PSEDITBOX_DISPONLY' id='DERIVED_SSS_SCL_UP_GRADE1$4'>998</span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divDERIVED_SSS_SCL_UP_GRANTOR_DESCR$4'><span    aria-disabled='true' class='PSEDITBOX_DISPONLY' id='DERIVED_SSS_SCL_UP_GRANTOR_DESCR$4'>Addmitted to sup</span>" +
                "</DIV></td>" +
                "</tr>" +
                "<tr id='trSTDNT_WEEK_SCHD$0_row6' valign='center' onClick=\"HighLightTR('rgb(238,238,238)','','trSTDNT_WEEK_SCHD$0_row6');\" onMouseOver=\"hoverLightTR('rgb(253,255,200)','',0,'trSTDNT_WEEK_SCHD$0_row6');\" onmouseout=\"hoverLightTR('rgb(253,255,200)','',1,'trSTDNT_WEEK_SCHD$0_row6');\">" +
                "<td align='left'  height='22' class='PSLEVEL3GRID PSGRIDFIRSTCOLUMN' >" +
                "<DIV    id='win0divCLASS_NAME$5'><span id='CLASS_NAME$span$5'  class='PSHYPERLINKDISABLED'  title='View Details' >ESP 411-E100<br />" +
                "EXM (2496)</span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divCLASS_NAME1$5'><span id='CLASS_NAME1$span$5'  class='PSHYPERLINK'  title='Schedule' ><a name='CLASS_NAME1$5' id='CLASS_NAME1$5'  ptlinktgt='pt_peoplecode' tabindex='67' onclick='javascript:cancelBubble(event);' href=\"javascript:submitAction_win0(document.win0,'CLASS_NAME1$5');\"  class='PSHYPERLINK' >DSP programming and app 411</a></span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divDERIVED_SSS_SCL_SESSION_CODE$5'><span    aria-disabled='true' class='PSDROPDOWNLIST_DISPONLY' id='DERIVED_SSS_SCL_SESSION_CODE$5'>First Semester</span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divOFFGRADE$5'><span    class='PSLONGEDITBOX' id='OFFGRADE$5'>&nbsp;</span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divDERIVED_SSS_SCL_UP_GRADE1$5'><span    aria-disabled='true' class='PSEDITBOX_DISPONLY' id='DERIVED_SSS_SCL_UP_GRADE1$5'>&nbsp;</span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divDERIVED_SSS_SCL_UP_GRANTOR_DESCR$5'><span    aria-disabled='true' class='PSEDITBOX_DISPONLY' id='DERIVED_SSS_SCL_UP_GRANTOR_DESCR$5'>Grade Outstanding</span>" +
                "</DIV></td>" +
                "</tr>" +
                "<tr id='trSTDNT_WEEK_SCHD$0_row7' valign='center' onClick=\"HighLightTR('rgb(238,238,238)','','trSTDNT_WEEK_SCHD$0_row7');\" onMouseOver=\"hoverLightTR('rgb(253,255,200)','',0,'trSTDNT_WEEK_SCHD$0_row7');\" onmouseout=\"hoverLightTR('rgb(253,255,200)','',1,'trSTDNT_WEEK_SCHD$0_row7');\">" +
                "<td align='left'  height='22' class='PSLEVEL3GRID PSGRIDFIRSTCOLUMN' >" +
                "<DIV    id='win0divCLASS_NAME$6'><span id='CLASS_NAME$span$6'  class='PSHYPERLINKDISABLED'  title='View Details' >IPI 410-E100<br />" +
                "EXM (2304)</span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divCLASS_NAME1$6'><span id='CLASS_NAME1$span$6'  class='PSHYPERLINK'  title='Schedule' ><a name='CLASS_NAME1$6' id='CLASS_NAME1$6'  ptlinktgt='pt_peoplecode' tabindex='73' onclick='javascript:cancelBubble(event);' href=\"javascript:submitAction_win0(document.win0,'CLASS_NAME1$6');\"  class='PSHYPERLINK' >Engineer professionalism 410</a></span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divDERIVED_SSS_SCL_SESSION_CODE$6'><span    aria-disabled='true' class='PSDROPDOWNLIST_DISPONLY' id='DERIVED_SSS_SCL_SESSION_CODE$6'>First Semester</span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divOFFGRADE$6'><span    class='PSLONGEDITBOX' id='OFFGRADE$6'>&nbsp;</span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divDERIVED_SSS_SCL_UP_GRADE1$6'><span    aria-disabled='true' class='PSEDITBOX_DISPONLY' id='DERIVED_SSS_SCL_UP_GRADE1$6'>68</span>" +
                "</DIV></td>" +
                "<td class='PSLEVEL3GRID'  align='left'  class='PSLEVEL3GRID' >" +
                "<DIV    id='win0divDERIVED_SSS_SCL_UP_GRANTOR_DESCR$6'><span    aria-disabled='true' class='PSEDITBOX_DISPONLY' id='DERIVED_SSS_SCL_UP_GRANTOR_DESCR$6'>Pass</span>" +
                "</DIV></td>" +
                "</tr>" +
                "</table>" +
                "</DIV>" +
                "</td>" +
                "</tr>" +
                "<tr>" +
                "<td height='10' colspan='9'></td>" +
                "</tr>" +
                "</table>" +
                "</DIV>" +
                "<DIV class='' id='win0divPSTOOLBAR'><span style='margin: 5px'><a class='PSPUSHBUTTON Left' role='presentation'><span style='background-Color: transparent;border:0;'><input type='button' id='#ICCancel' name='#ICCancel'  class='PSPUSHBUTTONRETURN'   value='Return' onclick=\"javascript:submitAction_win0(document.win0, '#ICCancel');\" tabindex='350' alt='Return (Esc)' title='Return (Esc)'/> </span></a></span></DIV>" +
                "<DIV class='x'  id='pt_dragtxt' class='PSLEVEL1GRIDCOLUMNHDR'></div><div onmouseup='ptGridResizeObj_win0.TDselUp();' onmousemove='ptGridResizeObj_win0.TDselMove();' id='pt_dragResize' onmouseout='ptGridResizeObj_win0.dragTD=false;' onmbouseover='ptGridResizeObj_win0.dragTD=true;'></div>]]></FIELD>" +
                "<FIELD id='win0divPSPANELTABLINKS'><![CDATA[]]></FIELD>" +
                "<SYSVAR id='sysvar'><![CDATA[nMaxSavedStates=5;" +
                "sHistURL=\"https://upnet.up.ac.za/psc/pscsmpra/EMPLOYEE/HRMS/c/UP_SS_MENU.UP_SS_STUDENT.GBL?page=UP_SS_STUD_SR_SP&\";" +
                "bHtml5Doc = true;" +
                "bClearBackState=false;" +
                "bPageTransfered=false;" +
                "bTransferAnimate=false;" +
                "AddToHistory('UP Student Self Service', '', 'returntolastpage@0', 'UP_SS_STUD', 4, 0, 1, 0,'', 1, '', 0);" +
                "bCleanHtml = true;" +
                "bDefer = true;" +
                "document.hiddenFldArr_win0 =new Array('ICType','ICElementNum','ICStateNum','ICAction','ICXPos','ICYPos','ResponsetoDiffFrame','TargetFrameName','FacetPath','ICFocus','ICSaveWarningFilter','ICChanged','ICAutoSave','ICResubmit','ICSID','ICActionPrompt','ICBcDomData','ICPanelName','ICFind','ICAddCount','ICAPPCLSDATA');" +
                "document.chgFldArr_win0 = new Array();" +
                "bCDATA = false;" +
                "bAccessibleLayout = false;" +
                "bGenDomInfo = false;" +
                "]]></SYSVAR></PAGE>" +
                "";


        GetMarksTask(String _sNumber, String _pwd) {
            sNumber = _sNumber.substring(1);
            pwd = _pwd;
            portal = new UPPortal();
        }

        @Override
        protected String doInBackground(Void ... params) {
            String result;
            if (sNumber.equals("12345678")) {
                return XML;
            }

            try {
                publishProgress("Loggin in.");
                String loginAttemp = portal.login(sNumber, pwd);
                if (loginAttemp.equals("Network")) {
                    loginAttemp = portal.login(sNumber, pwd);
                }

                if (loginAttemp.equals("Success")) {
                    publishProgress("Retrieving marks.");
                    result = portal.getMarks();
                    if (result.startsWith("<?xml") == false) {
                        result = portal.getMarks();
                    }
                } else if (loginAttemp.equals("Network")) {
                    publishProgress("Network error, Check Internet Connection.");
                    return null;
                } else {
                    publishProgress(loginAttemp);
                    return null;
                }
            } catch (Exception e) {
                publishProgress("Network error, Check Internet Connection.");
                return null;
            } finally {
                portal.logout();
            }

            return result;
        }

        protected void onProgressUpdate(String... progress) {
            Toast.makeText(getBaseContext(), progress[0], Toast.LENGTH_SHORT).show();
        }

        protected void onPostExecute(String result) {
            if (result ==  null) {
                showProgress(false);
                return;
            }

            if (result.startsWith("<?xml")) {
                showProgress(false);
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("XML", result);
                startActivity(intent);
            } else {
                showProgress(false);
                Toast.makeText(getBaseContext(), "Failed to retrieve marks.", Toast.LENGTH_LONG).show();
            }
        }
    }
}

