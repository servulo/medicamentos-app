import { Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ApiService, Dose, Medication } from '../../core/api/api';
import { PushRegistrationService } from './push-registration.service';

@Component({
  standalone:true,imports:[DatePipe],
  template:`<div class="page-title"><div><h1>Doses pendentes</h1><p>Registre ou adie as próximas doses.</p></div><button class="secondary" (click)="push.register()">Ativar notificações</button></div>
  @if(!items.length){<div class="empty">Nenhuma dose pendente.</div>}<div class="stack">@for(d of items;track d.id){<article><div><h2>{{name(d)}}</h2><p>{{d.scheduledAt|date:'dd/MM/yyyy HH:mm'}} · Adiada {{d.snoozeCount}} vez(es)</p></div>
  <div class="actions"><button (click)="act(d,'take')">Tomar</button><button class="danger" (click)="act(d,'skip')">Pular</button>
  <select #delay aria-label="Tempo para adiar"><option value="10">10 min</option><option value="30">30 min</option><option value="60">60 min</option></select><button class="secondary" (click)="act(d,'snooze',+delay.value)">Adiar</button></div></article>}</div>`
})
export class DosesPage implements OnInit{
  private readonly api=inject(ApiService);readonly push=inject(PushRegistrationService);items:Dose[]=[];meds:Medication[]=[];
  ngOnInit(){this.reload();this.api.medications().subscribe(v=>this.meds=v);}
  reload(){this.api.doses('PENDING').subscribe(v=>this.items=v);}
  name(d:Dose){return d.medicationName || this.meds.find(m=>m.id===d.medicationId)?.name||'Medicamento';}
  act(d:Dose,action:'take'|'skip'|'snooze',minutes?:number){this.api.doseAction(d.id,action,minutes as 10|30|60).subscribe(()=>this.reload());}
}
