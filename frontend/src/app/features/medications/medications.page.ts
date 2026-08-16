import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService, Medication } from '../../core/api/api';

@Component({
  standalone: true, imports: [RouterLink],
  template: `<div class="page-title"><div><h1>Medicamentos</h1><p>Catálogo, estoque e limite para compra.</p></div><a class="button" routerLink="/medicamentos/novo">Novo medicamento</a></div>
  @if(error){<p class="error">{{error}}</p>} @if(!items.length&&!error){<div class="empty">Nenhum medicamento cadastrado.</div>}
  <div class="grid">@for(item of items;track item.id){<article><div class="row"><h2>{{item.name}}</h2>@if(item.purchaseNeeded){<span class="badge warning">Comprar</span>}</div>
    <p>Unidade: {{item.unit}}</p><dl><div><dt>Estoque</dt><dd>{{item.stockQuantity}} {{item.unit}}</dd></div><div><dt>Alerta em</dt><dd>{{item.purchaseThresholdUnits}} {{item.unit}}</dd></div></dl>
    <a [routerLink]="['/medicamentos',item.id,'editar']">Editar</a> · <a [routerLink]="['/agendas/nova']" [queryParams]="{medicationId:item.id}">Criar agenda</a>
    · <button type="button" class="secondary compact" (click)="remove(item)">Excluir</button></article>}</div>`
})
export class MedicationsPage implements OnInit {
  readonly api = inject(ApiService); items: Medication[]=[]; error='';
  ngOnInit(){this.load();}
  load(){this.api.medications().subscribe({next:v=>this.items=v,error:e=>this.error=e.message});}
  remove(item: Medication) {
    if (!confirm(`Excluir "${item.name}"? O medicamento, todas as agendas e todo o histórico de doses serão apagados de forma permanente e não poderão ser recuperados.`)) return;
    this.api.deleteMedication(item.id).subscribe({
      next: () => this.load(),
      error: e => this.error = e.message
    });
  }
}

@Component({
  standalone: true, imports: [ReactiveFormsModule, RouterLink],
  template: `<div class="page-title"><h1>{{id?'Editar':'Novo'}} medicamento</h1><a routerLink="/medicamentos">Voltar</a></div>
  <form class="form-card" [formGroup]="form" (ngSubmit)="save()">
    <label>Nome<input formControlName="name"></label><div class="form-grid">
    <label>Unidade<input formControlName="unit" placeholder="comprimido, ml..."></label>
    <label>Estoque atual<input type="number" min="0" step="1" formControlName="stockQuantity"></label>
    <label>Alerta de compra (unidades)<input type="number" min="0" step="1" formControlName="purchaseThresholdUnits"></label></div>
    @if(error){<p class="error">{{error}}</p>}<button [disabled]="form.invalid">Salvar</button></form>`
})
export class MedicationFormPage implements OnInit {
  private readonly fb=inject(FormBuilder); private readonly api=inject(ApiService); private readonly route=inject(ActivatedRoute); private readonly router=inject(Router);
  id=''; error='';
  readonly form=this.fb.nonNullable.group({
    name:['',Validators.required],
    unit:['comprimido'],
    stockQuantity:[0,[Validators.required,Validators.min(0)]],
    purchaseThresholdUnits:[10,[Validators.required,Validators.min(0)]]
  });
  ngOnInit(){this.id=this.route.snapshot.paramMap.get('id')||'';if(this.id)this.api.medication(this.id).subscribe({next:v=>this.form.patchValue(v),error:e=>this.error=e.message});}
  save(){
    if(this.form.invalid)return;
    const raw=this.form.getRawValue();
    const payload={...raw,stockQuantity:Math.trunc(raw.stockQuantity),purchaseThresholdUnits:Math.trunc(raw.purchaseThresholdUnits)};
    const request=this.id?this.api.updateMedication(this.id,payload):this.api.createMedication(payload);
    request.subscribe({next:()=>void this.router.navigateByUrl('/medicamentos'),error:e=>this.error=e.message});
  }
}
