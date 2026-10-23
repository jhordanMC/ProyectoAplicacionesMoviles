from pathlib import Path
root=Path(r'D:\en html\Sistema de Presupuestos\frontend\src')
p=root/'App.tsx';s=p.read_text(encoding='utf-8');s=s.replace("const [client,setClient]=useState(''),","const [clientError,setClientError]=useState(false);\nconst [client,setClient]=useState(''),")
s=s.replace("if(!client.trim()){setNotice('Indica el cliente al que pertenece este presupuesto.');return}","if(!client.trim()){setClientError(true);setNotice('Indica el cliente al que pertenece este presupuesto.');const field=document.getElementById('budget-client');field?.scrollIntoView({behavior:'smooth',block:'center'});field?.focus({preventScroll:true});return}setClientError(false);")
s=s.replace('id="budget-client" maxLength','id="budget-client" aria-invalid={clientError} aria-describedby={clientError?\'client-error\':undefined} maxLength')
s=s.replace('onChange={e=>setClient(e.target.value)}','onChange={e=>{setClient(e.target.value);setClientError(false)}}')
s=s.replace('/><datalist id="known-clients">','/>{clientError&&<p id="client-error" className="field-error" role="alert">Escribe el nombre o razón social del cliente para generar el presupuesto.</p>}<datalist id="known-clients">')
p.write_text(s,encoding='utf-8')
p=root/'styles.css';s=p.read_text(encoding='utf-8')+'\n.field-error{color:#b32f44;font-size:12px;margin-top:8px}.client-input input[aria-invalid="true"]{border-color:#d94c63;background:#fff8f9}.client-input input[aria-invalid="true"]:focus{outline-color:#f2a4b0}\n';p.write_text(s,encoding='utf-8')
