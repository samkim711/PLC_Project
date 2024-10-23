package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        // First, declare globals (fields), then declare functions (methods)
        for (Ast.Field field: ast.getFields()) visit(field);
        for (Ast.Method method: ast.getMethods()) visit(method);

        // Retrieve the function from the scope
        Environment.Function mainFunction = scope.lookupFunction("main", 0);

        // Invoke the function with an empty list of arguments to get the result
        return mainFunction.invoke(new ArrayList<>());
    }

    @Override
    public Environment.PlcObject visit(Ast.Field ast) {
        Environment.PlcObject val = Environment.NIL;
        if (ast.getValue().isPresent()) val = visit(ast.getValue().get());
        scope.defineVariable(ast.getName(), ast.getConstant(), val);

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        // define function that will be called later
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args->{
            // create scope
            scope = new Scope(scope);

            // define parameters as variables
            for (int i = 0; i < ast.getParameters().size(); i++) {
                scope.defineVariable(ast.getParameters().get(i), false, args.get(i));
            }

            // run function
            try {
                for (Ast.Statement statement: ast.getStatements()) visit(statement);
                return Environment.NIL;
            }
            catch (Return r) {return r.value;} // handle return values
            finally {scope = scope.getParent();} // restore original scope
        });
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        Environment.PlcObject val = Environment.NIL;
        if (ast.getValue().isPresent()) val = visit(ast.getValue().get());
        scope.defineVariable(ast.getName(), false, val);

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        if (ast.getReceiver() instanceof Ast.Expression.Access){
            //TODO
        }
        else throw new RuntimeException("Not Access Type");
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.For ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        throw new Return(visit(ast.getValue()));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if (ast.getLiteral() == null) return Environment.NIL;
        return Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        if (ast.getReceiver().isPresent()) return visit(ast.getReceiver().get()).getField(ast.getName()).getValue();
        return scope.lookupVariable(ast.getName()).getValue();
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
