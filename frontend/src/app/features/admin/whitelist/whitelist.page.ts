import { Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService, MeService, WhitelistEntry } from '../../../core/api/api';

@Component({
  standalone:true,imports:[ReactiveFormsModule,DatePipe],
  template:`<div class="page-title"><div><h1>Usuários permitidos</h1><p>Gerencie a whitelist de acesso.</p></div></div>
  <form class="inline-form" (ngSubmit)="add()"><input type="email" [formControl]="email" placeholder="novo@exemplo.com"><button [disabled]="email.invalid">Adicionar</button></form>
  <div class="stack">@for(entry of items;track entry.id){<article class="row"><div><strong>{{entry.email}}</strong><p>Adicionado em {{entry.createdAt|date:'dd/MM/yyyy'}}</p></div>
  @if(entry.email!==me.profile()?.email){<button class="danger" (click)="remove(entry)">Remover</button>}@else{<span class="badge">Administrador</span>}</article>}</div>`
})
export class WhitelistPage implements OnInit{
  private readonly api=inject(ApiService);readonly me=inject(MeService);items:WhitelistEntry[]=[];
  readonly email=new FormControl('',{nonNullable:true,validators:[Validators.required,Validators.email]});
  ngOnInit(){this.load();}load(){this.api.whitelist().subscribe(v=>this.items=v);}
  add(){if(this.email.invalid)return;this.api.addWhitelist(this.email.value).subscribe(()=>{this.email.reset();this.load();});}
  remove(entry:WhitelistEntry){if(confirm(`Remover ${entry.email}?`))this.api.removeWhitelist(entry.id).subscribe(()=>this.load());}
}
