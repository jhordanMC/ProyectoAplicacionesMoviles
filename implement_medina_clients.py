from pathlib import Path
import re
root=Path(r'D:\en html\Sistema de Presupuestos')
p=root/'frontend/src/App.tsx';s=p.read_text(encoding='utf-8');s=s.replace("import {get} from './services/http';", "import {get} from './services/http';\nimport ImageAnalyzer from './features/ImageAnalyzer';")
s=s.replace('hours?:number;people?:string[]','hours?:number').replace('tax:boolean;status:string','tax:boolean;status:string;client?:string')
s=s.replace("const [people,setPeople]=useState<string[]>([]),[person,setPerson]=useState(''),[personModal,setPersonModal]=useState(false),", "const [client,setClient]=useState(''),[imageModal,setImageModal]=useState(false),")
s=s.replace('hours:Number(hours)||0,people:[...people]','hours:Number(hours)||0')
s=s.replace("const generate=()=>{if(","const generate=()=>{if(!client.trim()){setNotice('Indica el cliente al que pertenece este presupuesto.');return}if(")
s=s.replace("tax,status:'Pendiente'","tax,status:'Pendiente',client:client.trim()")
s=s.replace("b.id.toLowerCase().includes(search.toLowerCase())","(b.id+' '+(b.client||'')).toLowerCase().includes(search.toLowerCase())")
s=s.replace('Agrega las personas involucradas y completa los trabajos y materiales para generar un presupuesto.','Selecciona el cliente y completa los trabajos y materiales para generar su presupuesto.')
start=s.index('<section className="people-row">');end=s.index('<div className="panels">',start)
s=s[:start]+'''<section className="client-row"><span className="tile-icon"><Icon name="users" size={32}/></span><div className="client-input"><label htmlFor="budget-client">Cliente del presupuesto <span className="pill">Titular</span></label><input id="budget-client" maxLength={150} list="known-clients" value={client} onChange={e=>setClient(e.target.value)} placeholder="Nombre completo o razón social del cliente"/><datalist id="known-clients">{Array.from(new Set(budgets.map(b=>b.client).filter(Boolean))).map(n=><option key={n} value={n}/>)}</datalist></div><div className="client-summary"><small>Este presupuesto pertenece a</small><strong>{client.trim()||'Selecciona un cliente'}</strong></div></section>
'''+s[end:]
s=s.replace('<span className="pill">{people.length} personas agregadas</span>','')
s=re.sub(r'<div className="assigned">.*?</div>','',s)
s=s.replace('Analizar imagen con IA','Analizar imagen')
s=s.replace('Extrae materiales desde una imagen.<br/><span className="coming">Disponible en una próxima fase</span>','Lee listas o boletas con texto.<br/><span className="coming">OCR local · revisión antes de agregar</span>')
s=s.replace("onClick={()=>setNotice('El análisis con IA aún no está implementado.')}","onClick={()=>setImageModal(true)}")
s=s.replace('placeholder="Buscar por N° de presupuesto…"','placeholder="Buscar por N° de presupuesto o cliente…"')
s=s.replace('<th>N° Presupuesto</th><th>Fecha de emisión</th>','<th>N° Presupuesto</th><th>Cliente</th><th>Fecha de emisión</th>')
s=s.replace('</span></td><td>{new Date(b.date)',"</span></td><td>{b.client||'Sin cliente (anterior)'}</td><td>{new Date(b.date)")
start=s.index('{personModal&&');end=s.index('{preview&&',start)
s=s[:start]+'''{imageModal&&<ImageAnalyzer onClose={()=>setImageModal(false)} onImport={items=>{setLabor(previous=>[...previous,...items.filter(i=>i.kind==='labor').map(i=>({id:crypto.randomUUID(),name:i.name,amount:i.amount}))]);setMaterials(previous=>[...previous,...items.filter(i=>i.kind==='material').map(i=>({id:crypto.randomUUID(),name:i.name,amount:i.amount}))]);setImageModal(false);setNotice('Partidas revisadas agregadas al presupuesto.');}}/>}
'''+s[end:]
s=s.replace("{i.hours} h · {i.people?.join(', ')||'Sin personas asignadas'}",'{i.hours} horas estimadas')
p.write_text(s,encoding='utf-8')
p=root/'frontend/src/services/http.ts';s=p.read_text(encoding='utf-8');s+='''
export async function post<T>(path:string, body:unknown, signal?:AbortSignal):Promise<T>{
 const response=await fetch(`${base}/${path.replace(/^\\//,'')}`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body),signal});
 const data=await response.json().catch(()=>null);
 if(!response.ok)throw new Error(data?.error?.message||`No se pudo analizar la imagen (${response.status})`);
 return data as T;
}
''';p.write_text(s,encoding='utf-8')
print('Client ownership corrected; image analyzer wired')
