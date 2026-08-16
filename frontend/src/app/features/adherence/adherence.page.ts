import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, Dose, Medication } from '../../core/api/api';

@Component({
  standalone:true,imports:[DatePipe,FormsModule],
  template:`<div class="page-title"><div><h1>Histórico de adesão</h1><p>Doses tomadas e puladas.</p></div>
  <label class="inline">Medicamento<select [(ngModel)]="medicationId" (ngModelChange)="load()"><option value="">Todos</option>@for(m of meds;track m.id){<option [value]="m.id">{{m.name}}</option>}</select></label></div>
  <div class="stack">@for(d of items;track d.id){<article class="row"><div><h2>{{name(d)}}</h2><p>Prevista: {{d.scheduledAt|date:'dd/MM/yyyy HH:mm'}}</p></div>
  <span class="badge" [class.warning]="d.status==='SKIPPED'">{{d.status==='TAKEN'?'Tomada':'Pulada'}}</span></article>}</div>`
})
export class AdherencePage implements OnInit{
  private readonly api=inject(ApiService);items:Dose[]=[];meds:Medication[]=[];medicationId='';
  ngOnInit(){this.api.medications().subscribe(v=>this.meds=v);this.load();}
  load(){this.api.doses(undefined,this.medicationId||undefined).subscribe(v=>this.items=v.filter(d=>d.status!=='PENDING'));}
  name(d:Dose){return d.medicationName || this.meds.find(m=>m.id===d.medicationId)?.name || 'Medicamento';}
}
