#include "stdafx.h"
#include "CrypToolApp.h"
#include "CrypToolBase.h"
#include "DlgHashDemo.h"
#include "CrypToolTools.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

CDlgHashDemo::CDlgHashDemo(const CString &_documentFileName, const CString &_documentTitle, CWnd* pParent) :
	CDialog(CDlgHashDemo::IDD, pParent),
	m_documentFileName(_documentFileName),
	m_documentTitle(_documentTitle) {
	m_rb_DarstHW = 1;
	m_strOrigHash = _T("");
	m_strNewHash = _T("");
	m_strHashDiffRE = _T("");
}

CDlgHashDemo::~CDlgHashDemo() {
	
}

BOOL CDlgHashDemo::OnInitDialog() {
	CDialog::OnInitDialog();

	// load document specified at construction into temporary byte string
	CrypTool::ByteString byteStringTemporary;
	byteStringTemporary.readFromFile(m_documentFileName);
	// try to truncate the temporary byte string to the specified length limit, 
	// and also remove all bytes (including and) beyond the first null byte; if 
	// the byte string is modified in any way, the user is notified; if the 
	// resulting byte string is empty, the user is notified again and then the 
	// dialog is cancelled
	const unsigned int truncateAtLength = 16000;
	const bool truncateAtFirstNullByte = true;
	if (CrypTool::Utilities::truncateByteString(byteStringTemporary, truncateAtLength, truncateAtFirstNullByte) == 0) {
		CString message;
		message.Format("CRYPTOOL_BASE: string empty, dialog is closed");
		AfxMessageBox(message, MB_ICONEXCLAMATION);
		EndDialog(IDCANCEL);
	}
	// assign the temporary byte string to the internal byte string
	m_dataOrig = byteStringTemporary;

	// we also initialize the text window
	m_ctrlText.SetWindowText(m_dataOrig.toString());

	LOGFONT lf = { 14,0,0,0,FW_NORMAL,false,false,false,DEFAULT_CHARSET,OUT_CHARACTER_PRECIS,CLIP_CHARACTER_PRECIS,DEFAULT_QUALITY,DEFAULT_PITCH | FF_DONTCARE,"Courier" };
	m_font.CreateFontIndirect(&lf);

	CWnd* pStatic = GetDlgItem(IDC_EDIT_TEXT);
	CWnd* pStatic1 = GetDlgItem(IDC_EDIT_ORIGHASH);
	CWnd* pStatic2 = GetDlgItem(IDC_EDIT_AKTHASH);
	CWnd* pStatic3 = GetDlgItem(IDC_RICHEDIT_HASHDIFF);

	pStatic->SetFont(&m_font, false);
	pStatic1->SetFont(&m_font, false);
	pStatic2->SetFont(&m_font, false);
	pStatic3->SetFont(&m_font, false);

	// initialize the supported hash algorithm types
	vectorHashAlgorithmTypes.push_back(CrypTool::Cryptography::Hash::HASH_ALGORITHM_TYPE_MD4);
	vectorHashAlgorithmTypes.push_back(CrypTool::Cryptography::Hash::HASH_ALGORITHM_TYPE_MD5);
	vectorHashAlgorithmTypes.push_back(CrypTool::Cryptography::Hash::HASH_ALGORITHM_TYPE_SHA);
	vectorHashAlgorithmTypes.push_back(CrypTool::Cryptography::Hash::HASH_ALGORITHM_TYPE_SHA1);
	vectorHashAlgorithmTypes.push_back(CrypTool::Cryptography::Hash::HASH_ALGORITHM_TYPE_SHA256);
	vectorHashAlgorithmTypes.push_back(CrypTool::Cryptography::Hash::HASH_ALGORITHM_TYPE_SHA512);
	vectorHashAlgorithmTypes.push_back(CrypTool::Cryptography::Hash::HASH_ALGORITHM_TYPE_RIPEMD160);

	// initialize combo box with supported hash algorithm types
	for (size_t index = 0; index < vectorHashAlgorithmTypes.size(); index++) {
		const CrypTool::Cryptography::Hash::HashAlgorithmType hashAlgorithmType = vectorHashAlgorithmTypes[index];
		m_comboCtrlSelectHashFunction.AddString(CrypTool::Cryptography::Hash::getHashAlgorithmName(hashAlgorithmType));
	}

	// select MD4 by default
	m_comboCtrlSelectHashFunction.SetCurSel(0);
	OnSelendokComboSelectHashFunction();

	return TRUE;
}

void CDlgHashDemo::DoDataExchange(CDataExchange* pDX) {
	CDialog::DoDataExchange(pDX);
	DDX_Control(pDX, IDC_COMBO_SELECT_HASH_FUNCTION, m_comboCtrlSelectHashFunction);
	DDX_Control(pDX, IDC_EDIT_TEXT, m_ctrlText);
	DDX_Control(pDX, IDC_RICHEDIT_HASHDIFF, m_ctrlHashDiff);
	DDX_Radio(pDX, IDC_RADIO_DEC, m_rb_DarstHW);
	DDX_Text(pDX, IDC_EDIT_ORIGHASH, m_strOrigHash);
	DDX_Text(pDX, IDC_EDIT_AKTHASH, m_strNewHash);
}

void CDlgHashDemo::OnRadioBin() {
	UpdateData(true);
	m_strOrigHash = m_hash.toString(2, "#");
	m_strNewHash = m_newHash.toString(2, "#");
	UpdateData(false);
}

void CDlgHashDemo::OnRadioDec() {
	UpdateData(true);
	m_strOrigHash = m_hash.toString(10, " ");
	m_strNewHash = m_newHash.toString(10, " ");
	UpdateData(false);
}

void CDlgHashDemo::OnRadioHex() {
	UpdateData(true);
	m_strOrigHash = m_hash.toString(16, " ");
	m_strNewHash = m_newHash.toString(16, " ");
 	UpdateData(false);
}

void CDlgHashDemo::OnChangeEditText() {
	UpdateData(true);
	CString text;
	m_ctrlText.GetWindowText(text);
	CrypTool::ByteString message;
	message.fromBuffer((unsigned char*)(text.GetBuffer()), text.GetLength());
	ComputeHash(&message, &m_newHash);
	switch (m_rb_DarstHW) {
	case 0: m_strNewHash = m_newHash.toString(10, " "); break;
	case 1: m_strNewHash = m_newHash.toString(16, " "); break;
	case 2: m_strNewHash = m_newHash.toString(2, "#"); break;
	}
	SetHashDiff(m_hash, m_newHash);
	UpdateData(false);	
	SetRed();
}

void CDlgHashDemo::OnSelendokComboSelectHashFunction() {
	ComputeHash(&m_dataOrig, &m_hash);
	UpdateData(true);
	switch (m_rb_DarstHW) {
	case 0: m_strOrigHash = m_hash.toString(10, " "); break;
	case 1: m_strOrigHash = m_hash.toString(16, " "); break;
	case 2: m_strOrigHash = m_hash.toString(2, "#"); break;
	}
	UpdateData(false);
	OnChangeEditText();
	char strSelectedHash[21];
	m_comboCtrlSelectHashFunction.GetWindowText(strSelectedHash, 20);
	CString title;
	title.Format(IDS_STRING_Hashdemo_orighash, strSelectedHash);
	title += m_documentTitle;
	this->SetWindowText(title);
}

static DWORD CALLBACK EditStreamCallBack(DWORD dwCookie, LPBYTE pbBuff, LONG cb, LONG *pcb) {
	CString *pstr = (CString *)dwCookie;
	if (pstr->GetLength() < cb)
	{
		*pcb = pstr->GetLength();
		memcpy(pbBuff, (LPCSTR)*pstr, *pcb);
		pstr->Empty();
	}
	else
	{
		*pcb = cb;
		memcpy(pbBuff, (LPCSTR)*pstr, *pcb);
		*pstr = pstr->Right(pstr->GetLength() - cb);
	}
	return 0;
}

void CDlgHashDemo::SetRed() {
	CString rtfPrefix, rtfPostfix;
	rtfPrefix = "{\\rtf1\\ansi\\deff0\\deftab720{\\fonttbl{\\f0\\froman "
		"Courier New;}}\n{\\colortbl;\\red0\\green0\\blue0;\\red255\\green0\\blue0;}\n"
		"\\deflang1033\\pard\\tx360\\tx720\\tx1080\\tx1440\\tx1800"
		"\\tx2160\\tx2520\\tx2880\\tx3240\\tx3600\\tx3960\\tx4320"
		"\\tx4680\\tx5040\\tx5400\\tx5760\\tx6120"
		"\\tx6480\\plain\\f3\\fs20 ";
	rtfPostfix = "\n\\par }";

	m_ctrlHashDiff.ShowWindow(SW_HIDE);
	m_ctrlHashDiff.SetSel(0, -1);
	m_ctrlHashDiff.ReplaceSel("");

	static int c = 0;

	int laenge = m_strHashDiffRE.GetLength();

	int x = 0;

	// offset:	Speichert das Offset der l�ngsten unver�nderten Bitfolge
	// length:	Speichert die L�nge der l�ngsten unver�nderten Bitfolge
	int offset = 0;
	int length = 0;
	// o:		Speichert das Offset der aktuellen Bitfolge
	// l:		Speichert die L�nge der aktuellen Bitfolge
	int o = 0;
	int l = 0;
	// y:		Gibt die Anzahl der gez�hlten Bits wieder, d.h. l�sst die automatisch
	//			eingef�gten Trennzeichen (#) zwischen den 8er-Bitbl�cken aus
	int y = 0;

	CString b = "";
	int zero = 0, one = 0;
	if (m_ctrlHashDiff.m_hWnd != 0)
	{
		for (x = 0; x<laenge; x++)
		{
			// ...hier unterscheidet sich ein Bit.
			if (m_strHashDiffRE[x] == '1')
			{
				one++;
				b += "{\\cf2 1}";

				// Variablen sollen ausschliesslich dann neu gesetzt werden, wenn
				// es sich NICHT um ein Trennzeichen (#) handelt.
				if ((x + 1) % 9 != 0)
				{
					l = 0;
					y++;
				}
			}
			// ...hier ist das entsprechende Bit gleich.
			else
			{
				if (m_strHashDiffRE[x] == '0')
					zero++;
				b += m_strHashDiffRE[x];

				// Variablen sollen ausschliesslich dann neu gesetzt werden, wenn
				// es sich NICHT um ein Trennzeichen (#) handelt.
				if ((x + 1) % 9 != 0)
				{

					if (l == 0) o = y;
					l++;
					if (l>length)
					{
						length = l;
						offset = o;
					}
					y++;
				}
			}
		}

		b += "\n\\par ";
		CString ratio;
		CString percentage = createStringNumberWithDigitGrouping((100.0*one) / (one + zero));
		ratio.Format(IDS_HASH_DEMO_PERCENT, percentage, one, (one + zero));
		b += ratio;

		CString sequence;
		sequence.Format(IDS_HASH_DEMO_SEQUENCE, offset, length);

		// Neue Zeile beginnen und Angaben �ber Offset/L�nge ausgeben
		b += "\n\\par ";
		b += sequence;
	}

	m_ctrlHashDiff.ShowWindow(SW_SHOW);

	// The rtfString contains the word Bold in bold font.
	CString rtfString = rtfPrefix + b + rtfPostfix;
	EDITSTREAM es = { (DWORD)&rtfString, 0, EditStreamCallBack };

	// richEd is the rich edit control
	m_ctrlHashDiff.StreamIn(SF_RTF | SFF_SELECTION, es);
}

void CDlgHashDemo::SetHashDiff(CrypTool::ByteString &hash1, CrypTool::ByteString &hash2) {
	m_strOrigHashBin = hash1.toString(2, "#");
	m_strNewHashBin = hash2.toString(2, "#");

	m_strHashDiffRE = "";
	int iLaengeDerHW;
	iLaengeDerHW = m_strNewHashBin.GetLength();

	for (int i = 0; i<iLaengeDerHW; i++)
	{
		if (m_strNewHashBin[i] == m_strOrigHashBin[i] && m_strNewHashBin[i] != '#')
		{
			m_strHashDiffRE += "0";
		}
		else if (m_strNewHashBin[i] == '#')
		{
			m_strHashDiffRE += "#";
		}
		else
		{
			m_strHashDiffRE += "1";
		}
	}
}

void CDlgHashDemo::ComputeHash(CrypTool::ByteString *data, CrypTool::ByteString *hashValue) {
	// acquire the selected hash algorithm type
	const CrypTool::Cryptography::Hash::HashAlgorithmType hashAlgorithmType = getHashAlgorithmType();
	// calculate the hash value
	CrypTool::Cryptography::Hash::HashOperation hashOperation(hashAlgorithmType);
	hashOperation.executeOnByteStrings(*data, *hashValue);
}

CrypTool::Cryptography::Hash::HashAlgorithmType CDlgHashDemo::getHashAlgorithmType() const {
	CrypTool::Cryptography::Hash::HashAlgorithmType hashAlgorithmType = CrypTool::Cryptography::Hash::HASH_ALGORITHM_TYPE_NULL;
	const int currentSelection = m_comboCtrlSelectHashFunction.GetCurSel();
	if (currentSelection >= 0 && currentSelection < vectorHashAlgorithmTypes.size())
		hashAlgorithmType = vectorHashAlgorithmTypes[currentSelection];
	return hashAlgorithmType;
}

BEGIN_MESSAGE_MAP(CDlgHashDemo, CDialog)
	ON_BN_CLICKED(IDC_RADIO_BIN, OnRadioBin)
	ON_BN_CLICKED(IDC_RADIO_DEC, OnRadioDec)
	ON_BN_CLICKED(IDC_RADIO_HEX, OnRadioHex)
	ON_EN_CHANGE(IDC_EDIT_TEXT, OnChangeEditText)
	ON_CBN_SELENDOK(IDC_COMBO_SELECT_HASH_FUNCTION, OnSelendokComboSelectHashFunction)
END_MESSAGE_MAP()
