Component({properties:{task:{type:Object,value:{id:''}}},methods:{onOpen(){this.triggerEvent('open',{id:this.data.task.id});}}});
