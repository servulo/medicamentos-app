import { Component, OnInit, inject } from '@angular/core';
import { ApiService, PurchaseAlert } from '../../core/api/api';

@Component({
  standalone:true,
  template:`<div class="page-title"><div><h1>Lista de compras</h1><p>Medicamentos que atingiram o limite configurado em unidades.</p></div></div>
  @if(!items.length){<div class="empty">Nenhuma compra necessária.</div>}<div class="grid">@for(a of items;track a.medicationId){<article><span class="badge warning">Estoque baixo</span><h2>{{a.name}}</h2>
  <p>Estoque: <strong>{{a.stockQuantity}}</strong> {{a.unit}} · Limite: {{a.purchaseThresholdUnits}} {{a.unit}}</p></article>}</div>`
})
export class AlertsPage implements OnInit{private readonly api=inject(ApiService);items:PurchaseAlert[]=[];ngOnInit(){this.api.alerts().subscribe(v=>this.items=v);}}
