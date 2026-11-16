Component({properties:{loading:Boolean,title:String,description:String,action:String,error:Boolean},methods:{onAction(){this.triggerEvent('retry');}}});
